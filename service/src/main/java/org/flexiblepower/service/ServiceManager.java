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
import java.nio.channels.ClosedSelectorException;
import java.util.concurrent.ExecutionException;
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
public class ServiceManager<T> implements Closeable {

    private static final TimeUnit SECONDS = TimeUnit.SECONDS;
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
    private final Socket managementSocket;

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
        this.managementSocket = ZMQ.context(1).socket(ZMQ.REP);

        // this.managerThread = new Thread(() -> {
        final String listenAddr = "tcp://*:" + ServiceManager.MANAGEMENT_PORT;
        ServiceManager.log.info("Start listening thread on {}", listenAddr);

        // Receive timeout must be -1, making the recv() blocking until something is received
        this.managementSocket.setReceiveTimeOut(-1);
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
                    final Message msg = this.pbSerializer.deserialize(data);
                    response = this.handleServiceMessage(msg);
                } catch (final Exception e) {
                    ServiceManager.log.error("Exception handling message", e);
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
                    // We must send something to continue the REQ/REP pattern
                    this.managementSocket.send("Panic!".getBytes());
                } catch (final ClosedSelectorException | ZError.IOException e) {
                    // Socket is closed, we are stopped
                    ServiceManager.log.warn("Socket forcibly closed, stopping thread", e);
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
     */
    private Message handleServiceMessage(final Message msg) throws IOException,
            ServiceInvocationException,
            ConnectionModificationException,
            SerializationException {

        if (this.managedService == null) {
            return ErrorMessage.newBuilder()
                    .setDebugInformation(
                            "User service has not instantiated yet, perhaps there is a problem in the constructor")
                    .setProcessId(this.processId)
                    .build();
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
     */
    private Message handleResumeProcessMessage(final ResumeProcessMessage msg) throws ServiceInvocationException {
        Future<ProcessStateUpdateMessage> future;
        ServiceManager.log.info("Received ResumeProcessMessage for process {}", msg.getProcessId());
        this.processId = msg.getProcessId();
        try {
            final Serializable state = msg.getStateData().isEmpty() ? null
                    : this.javaIoSerializer.deserialize(msg.getStateData().toByteArray());
            future = this.serviceExecutor.submit(() -> {
                this.managedService.resumeFrom(state);
                return this.createProcessStateUpdateMessage(ProcessState.RUNNING);
            });
            return future.get(ServiceManager.SERVICE_IMPL_TIMEOUT_SECONDS, ServiceManager.SECONDS);
        } catch (final Exception e) {
            ServiceManager.log.error("Exception while resuming from suspended: {}", e.getMessage());
            ServiceManager.log.trace(e.getMessage(), e);
            return ServiceManager.createErrorMessage(this.processId, e);
        }
    }

    /**
     * @param parseFrom
     * @return
     * @throws ServiceInvocationException
     */
    private Message handleSetConfigMessage(final SetConfigMessage message) throws ServiceInvocationException {
        final Future<ProcessStateUpdateMessage> future;
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

        future = this.serviceExecutor.submit(() -> {
            if (!this.configured) {
                this.managedService.init(config);
                this.configured = true;
            } else {
                this.managedService.modify(config);
            }
            return this.createProcessStateUpdateMessage(ProcessState.RUNNING);
        });

        try {
            return future.get(ServiceManager.SERVICE_IMPL_TIMEOUT_SECONDS, ServiceManager.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            ServiceManager.log.error("Exception while handling config message: {}", e.getMessage());
            ServiceManager.log.trace(e.getMessage(), e);
            return ServiceManager.createErrorMessage(this.processId, e);
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

    private static ErrorMessage createErrorMessage(final String processId, final Exception e) {
        return ErrorMessage.newBuilder()
                .setDebugInformation(
                        e != null ? e.getClass().toString() + ": " + e.getMessage() : "Unknown exception occurred")
                .setProcessId(processId)
                .build();
    }

}