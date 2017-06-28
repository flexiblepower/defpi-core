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
import org.flexiblepower.proto.ConnectionProto.ConnectionHandshake;
import org.flexiblepower.proto.ConnectionProto.ConnectionMessage;
import org.flexiblepower.proto.ServiceProto.GoToProcessStateMessage;
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

import com.google.protobuf.InvalidProtocolBufferException;

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

    public static final byte[] SUCCESS = new byte[] {0};
    public static final byte[] FAILURE = new byte[] {1};
    public static final int MANAGEMENT_PORT = 4999;

    // private final Class<? extends Service> serviceClass;
    private boolean configured;
    private volatile boolean keepThreadAlive;
    private final Thread managerThread;

    private final ConnectionManager connectionManager;
    private final Service service;
    private final JavaIOSerializer javaIoSerializer = new JavaIOSerializer();
    private final ProtobufMessageSerializer<ConnectionHandshake> connectionHandshakeMessageSerializer = new ProtobufMessageSerializer<>();
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

        // Because when this exists, it is initializing
        this.configured = false;
        this.keepThreadAlive = true;
        this.managerThread = new Thread(() -> {
            while (this.keepThreadAlive) {
                try {
                    final byte[] data = this.managementSocket.recv();
                    if (data != null) {
                        this.managementSocket.send(this.handleServiceMessage(data));
                    }
                } catch (final ClosedSelectorException | ZError.IOException e) {
                    // Socket is closed, we are stopped
                    ServiceManager.log.warn("Socket forcibly closed, stopping thread");
                    break;
                } catch (final Exception e) {
                    ServiceManager.log.error("Exception handling message: {}", e.getMessage());
                    ServiceManager.log.trace("Exception handing message", e);
                    this.managementSocket.send(ServiceManager.FAILURE);
                }
            }
            ServiceManager.log.trace("End of thread");
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
     * @param data
     * @throws IOException
     * @throws ServiceInvocationException
     * @throws ConnectionModificationException
     */
    private byte[] handleServiceMessage(final byte[] data)
            throws IOException, ServiceInvocationException, ConnectionModificationException, SerializationException {
        try {
            final GoToProcessStateMessage msg = GoToProcessStateMessage.parseFrom(data);
            return this.handleGoToProcessStateMessage(msg);
            // } catch (final SerializationException e) {
            // throw new ServiceInvocationException("Unable to serialize message response: " + e.getMessage(), e);
        } catch (final InvalidProtocolBufferException e) {
            // Not this type of message, try next
        }

        try {
            final ResumeProcessMessage msg = ResumeProcessMessage.parseFrom(data);
            this.handleResumeProcessMessage(msg);
            return ServiceManager.SUCCESS;
        } catch (final InvalidProtocolBufferException e) {
            // Not this type of message, try next
        }

        try {
            final SetConfigMessage msg = SetConfigMessage.parseFrom(data);
            this.handleSetConfigMessage(msg);
            return ServiceManager.SUCCESS;
        } catch (final InvalidProtocolBufferException e) {
            // Not this type of message, try next
        }

        try {
            final ConnectionMessage msg = ConnectionMessage.parseFrom(data);
            final ConnectionHandshake response = this.connectionManager.handleConnectionMessage(msg);
            return this.connectionHandshakeMessageSerializer.serialize(response);
        } catch (final InvalidProtocolBufferException e) {
            // Not this type of message, try next
        }

        throw new InvalidProtocolBufferException("Invalid protobuf format");
    }

    /**
     * @param message
     * @throws ServiceInvocationException
     */
    private byte[] handleGoToProcessStateMessage(final GoToProcessStateMessage message)
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
            return ServiceManager.SUCCESS;

        case SUSPENDED:
            final Serializable state = this.service.suspend();
            this.connectionManager.close();
            this.keepThreadAlive = false;
            return this.javaIoSerializer.serialize(state);

        case TERMINATED:
            this.service.terminate();
            this.connectionManager.close();
            this.keepThreadAlive = false;
            return ServiceManager.SUCCESS;

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
    private void handleResumeProcessMessage(final ResumeProcessMessage msg) throws ServiceInvocationException {
        ServiceManager.log.info("Received ResumeProcessMessage for process {}", msg.getProcessId());
        ServiceManager.log.trace("Received message: {}", msg);

        try {
            final Serializable state = this.javaIoSerializer.deserialize(msg.getStateData().toByteArray());
            this.service.resumeFrom(state);
        } catch (final Exception e) {
            throw new ServiceInvocationException("Error while resuming process", e);
        }
    }

    /**
     * @param parseFrom
     * @return
     * @throws ServiceInvocationException
     */
    private void handleSetConfigMessage(final SetConfigMessage message) throws ServiceInvocationException {
        ServiceManager.log.info("Received SetConfigMessage for process {}", message.getProcessId());
        ServiceManager.log.debug("Properties to set: {}", message.getConfig());
        ServiceManager.log.trace("Received message: {}", message);

        final Properties props = new Properties();
        message.getConfig().forEach((key, value) -> {
            props.setProperty(key, value);
        });

        if (this.configured) {
            this.service.init(props);
            this.configured = true;
        } else {
            this.service.modify(props);
        }
    }

}
