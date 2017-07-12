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

    private static final Logger log = LoggerFactory.getLogger(ServiceManager.class);

    /**
     * The receive timeout of the managementsocket also determines how often the thread "checks" if the keepalive
     * boolean is still true
     */
    private static final int MANAGEMENT_SOCKET_RECEIVE_TIMEOUT = 100;

    public static final int MANAGEMENT_PORT = 4999;

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

    public ServiceManager(final Service service) {
        this.connectionManager = new ConnectionManager();
        this.service = service;
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
    private Message handleServiceMessage(final Message msg)
            throws IOException, ServiceInvocationException, ConnectionModificationException, SerializationException {

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
            throws ServiceInvocationException, SerializationException {
        ServiceManager.log.info("Received GoToProcessStateMessage for process {} -> {}",
                message.getProcessId(),
                message.getTargetState());
        ServiceManager.log.trace("Received message: {}", message);

        switch (message.getTargetState()) {
        case RUNNING:
            // This is basically a "force start" with no configuration
            this.service.init(new Properties());
            this.configured = true;
            return this.createProcessStateUpdateMessage(ProcessState.RUNNING);

        case SUSPENDED:
            final Serializable state = this.service.suspend();
            this.connectionManager.close();
            this.keepThreadAlive = false;
            return this.createProcessStateUpdateMessage(ProcessState.SUSPENDED, this.javaIoSerializer.serialize(state));

        case TERMINATED:
            this.service.terminate();
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
        ServiceManager.log.info("Received ResumeProcessMessage for process {}", msg.getProcessId());
        ServiceManager.log.trace("Received message: {}", msg);

        this.processId = msg.getProcessId();

        try {
            final Serializable state = this.javaIoSerializer.deserialize(msg.getStateData().toByteArray());
            this.service.resumeFrom(state);
            return this.createProcessStateUpdateMessage(ProcessState.RUNNING);
        } catch (final Exception e) {
            throw new ServiceInvocationException("Error while resuming process", e);
        }
    }

    /**
     * @param parseFrom
     * @return
     * @throws ServiceInvocationException
     */
    private Message handleSetConfigMessage(final SetConfigMessage message) throws ServiceInvocationException {
        ServiceManager.log.info("Received SetConfigMessage for process {}", message.getProcessId());
        ServiceManager.log.debug("Properties to set: {}", message.getConfigMap().toString());
        ServiceManager.log.trace("Received message: {}", message);

        this.processId = message.getProcessId();

        final Properties props = new Properties();
        message.getConfigMap().forEach((key, value) -> {
            props.setProperty(key, value);
        });

        if (!this.configured) {
            this.service.init(props);
            this.configured = true;
        } else {
            this.service.modify(props);
        }

        return this.createProcessStateUpdateMessage(ProcessState.RUNNING);
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
