/**
 * File ServiceManager.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.ClosedSelectorException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import zmq.ZError;

/**
 * ServiceManager
 *
 * @author coenvl
 * @version 0.1
 * @since May 10, 2017
 */
public class ServiceManager implements Closeable {

    /**
     *
     */
    private static final TimeUnit SECONDS = TimeUnit.SECONDS;
    private static final Logger log = LoggerFactory.getLogger(ServiceManager.class);

    /**
     * The receive timeout of the managementsocket also determines how often the thread "checks" if the keepalive
     * boolean is still true
     */
    private static final int MANAGEMENT_SOCKET_RECEIVE_TIMEOUT = 100;
    public static final int MANAGEMENT_PORT = 4999;
    private static final long SERVICE_IMPL_TIMEOUT_SECONDS = 1;

    // private final Class<? extends Service> serviceClass;
    private boolean configured;
    private volatile boolean keepThreadAlive;
    private String processId = "unknown";
    private final Thread managerThread;

    private final ConnectionManager connectionManager;
    private final Service service;
    private final JavaIOSerializer javaIoSerializer = new JavaIOSerializer();
    private final ProtobufMessageSerializer pbSerializer = new ProtobufMessageSerializer();
    private final Socket managementSocket;
    private static ExecutorService serviceExecutor = Executors.newSingleThreadExecutor();

    public ServiceManager() throws ServiceInvocationException {
        this(ServiceMain.createInstance(ServiceManager.serviceExecutor));
    }

    public ServiceManager(final Service service) throws ServiceInvocationException {
        this.service = service;
        this.connectionManager = new ConnectionManager(ServiceManager.serviceExecutor);

        this.managementSocket = ZMQ.context(1).socket(ZMQ.REP);

        // this.managerThread = new Thread(() -> {
        final String listenAddr = "tcp://*:" + ServiceManager.MANAGEMENT_PORT;
        ServiceManager.log.info("Start listening thread on {}", listenAddr);

        this.managementSocket.setReceiveTimeOut(ServiceManager.MANAGEMENT_SOCKET_RECEIVE_TIMEOUT);
        this.managementSocket.bind(listenAddr);

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
                // Handle the message
                Message response;
                try {
                    final byte[] data = this.managementSocket.recv();
                    if (data != null) {
                        final Message msg = this.pbSerializer.deserialize(data);
                        response = this.handleServiceMessage(msg);
                    } else {
                        // Not received anything, better luck next iteration
                        continue;
                    }
                } catch (final Exception e) {
                    ServiceManager.log.error("Exception handing message", e);
                    response = ErrorMessage.newBuilder()
                            .setProcessId(this.processId)
                            .setDebugInformation("Error during handling of message: " + e.getMessage())
                            .build();
                }

                // Now try to send the response
                try {
                    this.managementSocket.send(this.pbSerializer.serialize(response));
                } catch (final SerializationException e) {
                    ServiceManager.log
                            .error("Error during serialization of message type " + response.getClass().getSimpleName());
                } catch (final ClosedSelectorException | ZError.IOException e) {
                    // Socket is closed, we are stopped
                    ServiceManager.log.warn("Socket forcibly closed, stopping thread", e);
                    break;
                }
            }
            ServiceManager.log.trace("End of thread");
            this.connectionManager.close();
            this.managementSocket.close();
        }, "ServiceManager thread");

        this.managerThread.start();
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
        this.join();
        this.managementSocket.close();
        this.connectionManager.close();
    }

    /**
     * @param msg
     * @throws IOException
     * @throws ServiceInvocationException
     * @throws ConnectionModificationException
     */
    private Message handleServiceMessage(final Message msg) throws IOException,
            ServiceInvocationException,
            ConnectionModificationException,
            SerializationException {

        if (msg instanceof GoToProcessStateMessage) {
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
        Future<ProcessStateUpdateMessage> future;
        final String processId = message.getProcessId();
        ServiceManager.log
                .info("Received GoToProcessStateMessage for process {} -> {}", processId, message.getTargetState());
        ServiceManager.log.trace("Received message: {}", message);

        switch (message.getTargetState()) {
        case RUNNING:
            // This is basically a "force start" with no configuration
            future = ServiceManager.serviceExecutor.submit(() -> {
                ServiceManager.this.service.init(new Properties());
                ServiceManager.this.configured = true;
                return ServiceManager.this.createProcessStateUpdateMessage(ProcessState.RUNNING);
            });
            break;

        case SUSPENDED:
            future = ServiceManager.serviceExecutor.submit(() -> {
                final Serializable state = this.service.suspend();
                this.connectionManager.close();
                this.keepThreadAlive = false;
                return this.createProcessStateUpdateMessage(ProcessState.SUSPENDED,
                        this.javaIoSerializer.serialize(state));
            });
            break;

        case TERMINATED:
            future = ServiceManager.serviceExecutor.submit(() -> {
                this.service.terminate();
                this.connectionManager.close();
                this.keepThreadAlive = false;
                return this.createProcessStateUpdateMessage(ProcessState.TERMINATED);
            });
            break;

        case STARTING:
        case INITIALIZING:
        default:
            // The manager should not receive this type of messages
            throw new ServiceInvocationException("Invalid target state: " + message.getTargetState());
        }

        try {
            return future.get(ServiceManager.SERVICE_IMPL_TIMEOUT_SECONDS, ServiceManager.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return this.createErrorMessage(processId, e);
        }
    }

    /**
     * @param msg
     * @throws ServiceInvocationException
     */
    private Message handleResumeProcessMessage(final ResumeProcessMessage msg) throws ServiceInvocationException {
        Future<ProcessStateUpdateMessage> future;
        ServiceManager.log.info("Received ResumeProcessMessage for process {}", msg.getProcessId());
        ServiceManager.log.trace("Received message: {}", msg);
        final String processId = msg.getProcessId();

        try {
            final Serializable state = this.javaIoSerializer.deserialize(msg.getStateData().toByteArray());
            future = ServiceManager.serviceExecutor.submit(() -> {
                this.service.resumeFrom(state);
                return this.createProcessStateUpdateMessage(ProcessState.RUNNING);
            });
            return future.get(ServiceManager.SERVICE_IMPL_TIMEOUT_SECONDS, ServiceManager.SECONDS);
        } catch (final Exception e) {
            return this.createErrorMessage(processId, e);
        }
    }

    /**
     * @param parseFrom
     * @return
     * @throws ServiceInvocationException
     */
    private Message handleSetConfigMessage(final SetConfigMessage message) throws ServiceInvocationException {
        Future<ProcessStateUpdateMessage> future;
        ServiceManager.log.info("Received SetConfigMessage for process {}", message.getProcessId());
        ServiceManager.log.debug("Properties to set: {}", message.getConfigMap().toString());
        ServiceManager.log.trace("Received message: {}", message);

        this.processId = message.getProcessId();

        final Properties props = new Properties();
        message.getConfigMap().forEach((key, value) -> {
            props.setProperty(key, value);
        });

        future = ServiceManager.serviceExecutor.submit(() -> {
            if (!this.configured) {
                this.service.init(props);
                this.configured = true;
            } else {
                this.service.modify(props);
            }
            return this.createProcessStateUpdateMessage(ProcessState.RUNNING);
        });

        try {
            return future.get(ServiceManager.SERVICE_IMPL_TIMEOUT_SECONDS, ServiceManager.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return this.createErrorMessage(this.processId, e);
        }
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

    private ErrorMessage createErrorMessage(final String processId, final Exception e) {
        return ErrorMessage.newBuilder().setDebugInformation(e.getMessage()).setProcessId(processId).build();
    }

    @SuppressWarnings({"resource", "unused"})
    public static void main(final String[] args) throws ServiceInvocationException {
        // Launch new service manager
        new ServiceManager();
    }

}