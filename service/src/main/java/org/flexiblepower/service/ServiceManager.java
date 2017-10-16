/**
 * File ServiceManager.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.flexiblepower.commons.TCPSocket;
import org.flexiblepower.exceptions.SerializationException;
import org.flexiblepower.proto.ConnectionProto.ConnectionMessage;
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
 * @author coenvl
 * @version 0.1
 * @since May 10, 2017
 */
public class ServiceManager<T> implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(ServiceManager.class);

    /**
     * The receive timeout of the managementsocket also determines how often the thread "checks" if the keepalive
     * boolean is still true
     */
    private static final long SERVICE_IMPL_TIMEOUT_SECONDS = 5;
    public static final int MANAGEMENT_PORT = 4999;
    private static int threadCount = 0;

    private final ServiceExecutor serviceExecutor;
    private final Thread managerThread;
    private final ConnectionManager connectionManager;
    private final JavaIOSerializer javaIoSerializer = new JavaIOSerializer();
    private final ProtobufMessageSerializer pbSerializer = new ProtobufMessageSerializer();
    private final TCPSocket managementSocket;

    private Service<T> managedService;
    private Class<T> configClass;
    private String processId = "unknown";
    private boolean configured;

    private volatile boolean keepThreadAlive;

    public ServiceManager(final Service<T> service) throws ServiceInvocationException {
        this();
        this.start(service);
    }

    public ServiceManager() {
        this.serviceExecutor = ServiceExecutor.getInstance();

        this.connectionManager = new ConnectionManager();
        ServiceManager.log.info("Start listening thread on {}", ServiceManager.MANAGEMENT_PORT);
        this.managementSocket = TCPSocket.asServer(ServiceManager.MANAGEMENT_PORT);

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
                    messageArray = this.managementSocket.read();
                } catch (IOException | InterruptedException e) {
                    if (this.keepThreadAlive) {
                        ServiceManager.log.warn("Socket closed while expecting instruction, stopping thread", e);
                    }
                    break;
                }

                // Handle the message
                Message response;
                try {
                    final Message msg = this.pbSerializer.deserialize(messageArray);
                    response = this.handleServiceMessage(msg);
                } catch (final Exception e) {
                    ServiceManager.log.error("Exception handling message", e);
                    response = ErrorMessage.newBuilder()
                            .setProcessId(this.processId)
                            .setDebugInformation("Error handling message: " + e.getMessage())
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
                } catch (final IOException | InterruptedException e) {
                    // Socket is closed, we are stopped
                    if (this.keepThreadAlive) {
                        ServiceManager.log.warn("Socket closed while sending reply, stopping thread", e);
                    }
                    break;
                }
            }

            ServiceManager.log.trace("End of thread");
            this.connectionManager.close();
            this.managementSocket.close();
        }, "dEF-Pi srvManThread-" + ServiceManager.threadCount++);
        this.managerThread.start();
    }

    @SuppressWarnings("unchecked")
    public void start(final Service<T> service) throws ServiceInvocationException {
        this.managedService = service;

        Class<T> clazz = null;
        for (final Method m : service.getClass().getMethods()) {
            if (m.getName().startsWith("init") && (m.getParameterTypes().length == 1)
                    && (m.getParameterTypes()[0].isInterface() || m.getParameterTypes()[0].equals(Void.class))) {
                clazz = (Class<T>) m.getParameterTypes()[0];
                break;
            }
        }
        if (clazz == null) {
            throw new ServiceInvocationException("Unable to find init() method for configuration");
        }
        this.configClass = clazz;
    }

    /**
     * Wait for the service management thread to stop. This function is called when we want to have the main thread wait
     * for the message handler thread to finish. i.e.
     * wait until a nice terminate message has arrived.
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
        this.managementSocket.close();
        this.connectionManager.close();
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
     */
    private Message handleGoToProcessStateMessage(final GoToProcessStateMessage message)
            throws ServiceInvocationException,
            SerializationException {
        ServiceManager.log.debug("Received GoToProcessStateMessage for process {} -> {}",
                message.getProcessId(),
                message.getTargetState());

        switch (message.getTargetState()) {
        case RUNNING:
            // This is basically a "force start" with no configuration
            this.serviceExecutor.submit(() -> {
                try {
                    this.managedService.init(null);
                } catch (final Throwable t) {
                    ServiceManager.log.error("Error while calling init() without config", t);
                }
            });
            ServiceManager.this.configured = true;
            return ServiceManager.this.createProcessStateUpdateMessage(ProcessState.RUNNING);
        case SUSPENDED:
            final Future<Serializable> future = this.serviceExecutor.submit(() -> {
                try {
                    return this.managedService.suspend();
                } catch (final Throwable t) {
                    ServiceManager.log.error("Error while calling suspend()", t);
                    return null;
                }
            });
            this.connectionManager.close();
            this.keepThreadAlive = false;

            byte[] stateData = null;
            try {
                stateData = this.javaIoSerializer
                        .serialize(future.get(ServiceManager.SERVICE_IMPL_TIMEOUT_SECONDS, TimeUnit.SECONDS));
            } catch (final TimeoutException | InterruptedException | ExecutionException e) {
                ServiceManager.log.error("Calling suspend() method took too much time");
            }
            return this.createProcessStateUpdateMessage(ProcessState.SUSPENDED, stateData);
        case TERMINATED:
            this.serviceExecutor.submit(() -> {
                try {
                    this.managedService.terminate();
                } catch (final Throwable t) {
                    ServiceManager.log.error("Error while calling terminate()", t);
                }
            });
            this.connectionManager.close();
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
        this.processId = msg.getProcessId();

        final Serializable state = msg.getStateData().isEmpty() ? null
                : this.javaIoSerializer.deserialize(msg.getStateData().toByteArray());
        final Future<ProcessStateUpdateMessage> future = this.serviceExecutor.submit(() -> {
            this.managedService.resumeFrom(state);
            return this.createProcessStateUpdateMessage(ProcessState.RUNNING);
        });

        return future.get(ServiceManager.SERVICE_IMPL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
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
        ServiceManager.log
                .debug("Properties to set: {} (update: {})", message.getConfigMap().toString(), message.getIsUpdate());

        if (this.configured != message.getIsUpdate()) {
            ServiceManager.log.warn(
                    "Incongruence detected in message.isUpdate ({}) and service configuration state ({})",
                    message.getIsUpdate(),
                    this.configured);
        }

        this.processId = message.getProcessId();
        final T config = ServiceConfig.generateConfig(this.configClass, message.getConfigMap());

        final Future<ProcessStateUpdateMessage> future = this.serviceExecutor.submit(() -> {
            if (!this.configured) {
                this.managedService.init(config);
                this.configured = true;
            } else {
                this.managedService.modify(config);
            }
            return this.createProcessStateUpdateMessage(ProcessState.RUNNING);
        });

        return future.get(ServiceManager.SERVICE_IMPL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
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
                .setProcessId(this.processId)
                .setState(processState)
                .setStateData(byteString)
                .build();
    }

}