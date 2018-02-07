/**
 * File ServiceManager.java
 *
 * Copyright 2017 FAN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flexiblepower.service;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.HttpClientBuilder;
import org.flexiblepower.commons.TCPSocket;
import org.flexiblepower.exceptions.SerializationException;
import org.flexiblepower.proto.ConnectionProto.ConnectionMessage;
import org.flexiblepower.proto.DefPiParams;
import org.flexiblepower.proto.ServiceProto.ErrorMessage;
import org.flexiblepower.proto.ServiceProto.GoToProcessStateMessage;
import org.flexiblepower.proto.ServiceProto.ProcessState;
import org.flexiblepower.proto.ServiceProto.ProcessStateUpdateMessage;
import org.flexiblepower.proto.ServiceProto.ResumeProcessMessage;
import org.flexiblepower.proto.ServiceProto.SetConfigMessage;
import org.flexiblepower.serializers.JavaIOSerializer;
import org.flexiblepower.serializers.ProtobufMessageSerializer;
import org.flexiblepower.service.exceptions.ConnectionModificationException;
import org.flexiblepower.service.exceptions.ServiceInvocationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

/**
 * ServiceManager
 *
 * @version 0.1
 * @param <T> The type of service this manager will maintain
 * @since May 10, 2017
 */
public class ServiceManager<T> implements Closeable {

    /**
     * The global port number on which to listen for management messages
     */
    public static final int MANAGEMENT_PORT = 4999;

    /**
     * The receive timeout of the managementsocket also determines how often the thread "checks" if the keepalive
     * boolean is still true
     */
    private static final long SOCKET_READ_TIMEOUT_MILLIS = Duration.ofSeconds(10).toMillis();
    private static final long SERVICE_IMPL_TIMEOUT_MILLIS = Duration.ofSeconds(5).toMillis();
    private static final Logger log = LoggerFactory.getLogger(ServiceManager.class);
    private static int threadCount = 0;

    private final ServiceExecutor serviceExecutor;
    private final Thread managerThread;
    private final ConnectionManager connectionManager;
    private final JavaIOSerializer javaIoSerializer = new JavaIOSerializer();
    private final ProtobufMessageSerializer pbSerializer = new ProtobufMessageSerializer();
    private final DefPiParameters defPiParams;

    private TCPSocket managementSocket;
    private Service<T> managedService;
    private Class<T> configClass;
    private boolean configured;

    private volatile boolean keepThreadAlive;

    /**
     * Create a new ServiceManager and the corresponding thread to communicate with the orchestrator. This does NOT use
     * the service implementation to avoid any errors that may occur in its constructor. Moreover, to properly start it,
     * we need its configuration, which we also must get from the orchestrator first. To start the service use
     * {@link #start(Service)}
     */
    ServiceManager() {
        this.serviceExecutor = ServiceExecutor.getInstance();

        this.connectionManager = new ConnectionManager();
        ServiceManager.log.info("Start listening thread on {}", ServiceManager.MANAGEMENT_PORT);
        this.managementSocket = TCPSocket.asServer(ServiceManager.MANAGEMENT_PORT);

        this.defPiParams = ServiceManager.generateDefPiParameters();

        // Initializer the ProtoBufe message serializer
        this.pbSerializer.addMessageClass(GoToProcessStateMessage.class);
        this.pbSerializer.addMessageClass(SetConfigMessage.class);
        this.pbSerializer.addMessageClass(ProcessStateUpdateMessage.class);
        this.pbSerializer.addMessageClass(ResumeProcessMessage.class);
        this.pbSerializer.addMessageClass(ConnectionMessage.class);

        // Because when this exists, it is initializing
        this.configured = false;
        this.keepThreadAlive = true;
        this.managerThread = new Thread(() -> {
            while (this.keepThreadAlive) {
                byte[] messageArray;
                try {
                    messageArray = this.managementSocket.read(ServiceManager.SOCKET_READ_TIMEOUT_MILLIS);
                    if (messageArray == null) {
                        // No message received...
                        continue;
                    }
                } catch (final IOException e) {
                    if (this.keepThreadAlive) {
                        ServiceManager.log.warn("Socket closed while expecting instruction, re-opening it", e);
                        this.managementSocket.close();
                        this.managementSocket = TCPSocket.asServer(ServiceManager.MANAGEMENT_PORT);
                        continue;
                    }
                    break;
                }

                // Handle the message
                Message response;
                try {
                    final Message msg = this.pbSerializer.deserialize(messageArray);
                    response = this.handleServiceMessage(msg);
                } catch (final Exception e) {
                    ServiceManager.log.error("Exception handling message: {}", e.getMessage());
                    ServiceManager.log.trace(e.getMessage(), e);
                    final StringWriter sw = new StringWriter();
                    final PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    response = ErrorMessage.newBuilder()
                            .setProcessId(this.getProcessId())
                            .setDebugInformation(sw.toString())
                            .build();
                }

                byte[] responseArray;
                try {
                    responseArray = this.pbSerializer.serialize(response);
                } catch (final SerializationException e) {
                    responseArray = "Serialization error in servicemanager".getBytes();
                    ServiceManager.log
                            .error("Error during serialization of message type " + response.getClass().getSimpleName());
                }

                // Now try to send the response
                try {
                    this.managementSocket.send(responseArray);
                } catch (final IOException e) {
                    // Socket is closed, we are stopped
                    if (this.keepThreadAlive) {
                        ServiceManager.log.warn("Socket closed while sending reply, re-opening it", e);
                        this.managementSocket.close();
                        this.managementSocket = TCPSocket.asServer(ServiceManager.MANAGEMENT_PORT);
                    } else {
                        break;
                    }
                }
            }

            ServiceManager.log.trace("End of thread");
            this.connectionManager.close();
            this.managementSocket.close();
        }, "dEF-Pi srvManThread-" + ServiceManager.threadCount++);
        this.managerThread.start();
    }

    /**
     * @return
     */
    private String getProcessId() {
        final String processId = this.defPiParams.getProcessId();
        return processId != null ? processId : "null";
    }

    /**
     * Start the servicemanager for the constructed service. This function will initiate the communication with the
     * orchestrator to obtain the configuration for the service. It uses reflection to determine the exact type of
     * configuration we need to build and provide to the service.
     *
     * @param service The service to manage
     * @throws ServiceInvocationException if we cannot find the {@link Service#init(Object, DefPiParameters)} method
     */
    @SuppressWarnings("unchecked")
    void start(final Service<T> service) throws ServiceInvocationException {
        this.managedService = service;

        Class<T> clazz = null;
        for (final Method m : service.getClass().getMethods()) {
            if (m.getName().startsWith("init") && (m.getParameterTypes().length == 2)
                    && (m.getParameterTypes()[0].isInterface() || m.getParameterTypes()[0].equals(Void.class))) {
                clazz = (Class<T>) m.getParameterTypes()[0];
                break;
            }
        }
        if (clazz == null) {
            throw new ServiceInvocationException("Unable to find init() method for configuration");
        }
        this.configClass = clazz;

        // Send a request to the orchestrator that we are waiting for him
        this.requestConfig();
    }

    /**
     * @throws ServiceInvocationException
     *
     */
    @SuppressWarnings("resource")
    private void requestConfig() throws ServiceInvocationException {
        try {
            final URI uri = new URI("http",
                    null,
                    this.defPiParams.getOrchestratorHost(),
                    this.defPiParams.getOrchestratorPort(),
                    "/process/trigger/" + this.defPiParams.getProcessId(),
                    null,
                    null);
            ServiceManager.log.info("Requesting config message from orchestrator at {}", uri);

            final HttpClient client = HttpClientBuilder.create().build();

            // Create http request with token in header
            final HttpPut request = new HttpPut(uri);
            request.addHeader("X-Auth-Token", this.defPiParams.getOrchestratorToken());

            final HttpResponse response = client.execute(request);
            if (response.getStatusLine().getStatusCode() != 204) {
                throw new ServiceInvocationException(
                        "Unable to request config: " + response.getStatusLine().getReasonPhrase());
            }
        } catch (final URISyntaxException | IOException e) {
            throw new ServiceInvocationException(
                    "Futile to start service without triggering process config at orchestrator.",
                    e);
        }
    }

    /**
     * Wait for the service management thread to stop. This function is called when we want to have the main thread wait
     * for the message handler thread to finish. i.e. wait until a nice terminate message has arrived.
     */
    void join() {
        try {
            ServiceManager.log.info("Waiting for service thread to stop...");
            if (this.managerThread.isAlive()) {
                this.managerThread.join();
            }
        } catch (final InterruptedException e) {
            ServiceManager.log.info("Interuption exception received, stopping...");
        }
    }

    @Override
    public void close() {
        this.keepThreadAlive = false;

        if (this.managementSocket != null) {
            this.managementSocket.close();
        }

        this.connectionManager.close();
        this.serviceExecutor.shutDown();
    }

    /**
     * @param msg
     * @throws IOException
     * @throws ServiceInvocationException
     * @throws ConnectionModificationException
     * @throws TimeoutException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private Message handleServiceMessage(final Message msg) throws ServiceInvocationException,
            ConnectionModificationException,
            SerializationException,
            InterruptedException,
            ExecutionException,
            TimeoutException,
            IOException {

        if (this.managedService == null) {
            throw new ServiceInvocationException(
                    "User service has not instantiated yet, perhaps there is a problem in the constructor");
        } else if (msg instanceof GoToProcessStateMessage) {
            return this.handleGoToProcessStateMessage((GoToProcessStateMessage) msg);
        } else if (msg instanceof ResumeProcessMessage) {
            return this.handleResumeProcessMessage((ResumeProcessMessage) msg);
        } else if (msg instanceof SetConfigMessage) {
            return this.handleSetConfigMessage((SetConfigMessage) msg);
        } else if (msg instanceof ConnectionMessage) {
            return this.connectionManager.handleConnectionMessage((ConnectionMessage) msg);
        }

        throw new InvalidProtocolBufferException("Received unknown message, type: " + msg.getClass().getName());
    }

    /**
     * @param message
     * @throws ServiceInvocationException
     * @throws TimeoutException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private Message handleGoToProcessStateMessage(final GoToProcessStateMessage message)
            throws ServiceInvocationException,
            SerializationException,
            InterruptedException,
            ExecutionException,
            TimeoutException {
        if ((this.defPiParams.getProcessId() != null)
                && !message.getProcessId().equals(this.defPiParams.getProcessId())) {
            throw new ServiceInvocationException(
                    "Received message for unexpected process id " + message.getProcessId());
        }

        ServiceManager.log.debug("Received GoToProcessStateMessage for process {} -> {}",
                message.getProcessId(),
                message.getTargetState());

        switch (message.getTargetState()) {
        case RUNNING:
            // This is basically a "force start" with no configuration
            final Future<ProcessStateUpdateMessage> startFuture = this.serviceExecutor.submit(() -> {
                this.managedService.init(null, this.defPiParams);
                ServiceManager.this.configured = true;
                return ServiceManager.this.createProcessStateUpdateMessage(ProcessState.RUNNING);
            });

            return startFuture.get(ServiceManager.SERVICE_IMPL_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        case SUSPENDED:
            final Future<Serializable> suspendFuture = this.serviceExecutor.submit(() -> {
                return this.managedService.suspend();
            });
            final Serializable state = suspendFuture.get(ServiceManager.SERVICE_IMPL_TIMEOUT_MILLIS,
                    TimeUnit.MILLISECONDS);
            this.keepThreadAlive = false;

            return this.createProcessStateUpdateMessage(ProcessState.SUSPENDED, this.javaIoSerializer.serialize(state));
        case TERMINATED:
            this.serviceExecutor.submit(() -> {
                try {
                    this.managedService.terminate();
                } catch (final Throwable t) {
                    ServiceManager.log.error("Error while calling terminate()", t);
                }
            });

            // Connections are closed by the manager thread
            this.keepThreadAlive = false;
            return this.createProcessStateUpdateMessage(ProcessState.TERMINATED);
        case STARTING:
        case INITIALIZING:
        default:
            // The manager should not receive this type of messages
            throw new ServiceInvocationException("Invalid target state: " + message.getTargetState());
        }
    }

    /**
     * @param msg
     * @throws ServiceInvocationException
     * @throws SerializationException
     * @throws TimeoutException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private Message handleResumeProcessMessage(final ResumeProcessMessage msg) throws ServiceInvocationException,
            SerializationException,
            InterruptedException,
            ExecutionException,
            TimeoutException {
        ServiceManager.log.info("Received ResumeProcessMessage for process {}", msg.getProcessId());
        if ((this.defPiParams.getProcessId() != null) && !msg.getProcessId().equals(this.defPiParams.getProcessId())) {
            throw new ServiceInvocationException("Received message for unexpected process id " + msg.getProcessId());
        }

        final Serializable state = msg.getStateData().isEmpty() ? null
                : this.javaIoSerializer.deserialize(msg.getStateData().toByteArray());
        final Future<ProcessStateUpdateMessage> future = this.serviceExecutor.submit(() -> {
            this.managedService.resumeFrom(state);
            return this.createProcessStateUpdateMessage(ProcessState.RUNNING);
        });

        return future.get(ServiceManager.SERVICE_IMPL_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    /**
     * @param parseFrom
     * @return
     * @throws ServiceInvocationException
     * @throws TimeoutException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private Message handleSetConfigMessage(final SetConfigMessage message) throws ServiceInvocationException,
            InterruptedException,
            ExecutionException,
            TimeoutException {
        ServiceManager.log.info("Received SetConfigMessage for process {}", message.getProcessId());

        if ((this.defPiParams.getProcessId() != null)
                && !message.getProcessId().equals(this.defPiParams.getProcessId())) {
            throw new ServiceInvocationException(
                    "Received message for unexpected process id " + message.getProcessId());
        }

        ServiceManager.log
                .debug("Properties to set: {} (update: {})", message.getConfigMap().toString(), message.getIsUpdate());

        if (this.configured != message.getIsUpdate()) {
            ServiceManager.log.warn(
                    "Incongruence detected in message.isUpdate ({}) and service configuration state ({})",
                    message.getIsUpdate(),
                    this.configured);
        }

        final T config = ServiceConfig.generateConfig(this.configClass, message.getConfigMap());

        final Future<ProcessStateUpdateMessage> configFuture = this.serviceExecutor.submit(() -> {
            if (!this.configured) {
                this.managedService.init(config, this.defPiParams);
                this.configured = true;
            } else {
                this.managedService.modify(config);
            }
            return this.createProcessStateUpdateMessage(ProcessState.RUNNING);
        });

        return configFuture.get(ServiceManager.SERVICE_IMPL_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    /**
     * @param params
     * @return
     */
    private static DefPiParameters generateDefPiParameters() {
        int orchestratorPort = 0;
        try {
            orchestratorPort = Integer
                    .parseInt(System.getenv().getOrDefault(DefPiParams.ORCHESTRATOR_PORT.name(), "0"));
        } catch (final NumberFormatException e) {
            // 0 is the default value
        }
        return new DefPiParameters(System.getenv().getOrDefault(DefPiParams.ORCHESTRATOR_HOST.name(), null),
                orchestratorPort,
                System.getenv().getOrDefault(DefPiParams.ORCHESTRATOR_TOKEN.name(), null),
                System.getenv().getOrDefault(DefPiParams.PROCESS_ID.name(), null),
                System.getenv().getOrDefault(DefPiParams.USER_ID.name(), null),
                System.getenv().getOrDefault(DefPiParams.USER_NAME.name(), null),
                System.getenv().getOrDefault(DefPiParams.USER_EMAIL.name(), null));
    }

    private ProcessStateUpdateMessage createProcessStateUpdateMessage(final ProcessState processState) {
        return this.createProcessStateUpdateMessage(processState, null);
    }

    private ProcessStateUpdateMessage createProcessStateUpdateMessage(final ProcessState processState,
            final byte[] data) {
        ByteString byteString;
        if ((data == null) || (data.length == 0)) {
            byteString = ByteString.EMPTY;
        } else {
            byteString = ByteString.copyFrom(data);
        }
        return ProcessStateUpdateMessage.newBuilder()
                .setProcessId(this.getProcessId())
                .setState(processState)
                .setStateData(byteString)
                .build();
    }

}