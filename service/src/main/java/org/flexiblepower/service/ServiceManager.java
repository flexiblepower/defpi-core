/**
 * File ServiceManager.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.ClosedSelectorException;
import java.util.Properties;

import org.flexiblepower.service.exceptions.ConnectionModificationException;
import org.flexiblepower.service.exceptions.SerializationException;
import org.flexiblepower.service.exceptions.ServiceInvocationException;
import org.flexiblepower.service.proto.ServiceProto.ConnectionMessage;
import org.flexiblepower.service.proto.ServiceProto.GoToProcessStateMessage;
import org.flexiblepower.service.proto.ServiceProto.ResumeProcessMessage;
import org.flexiblepower.service.proto.ServiceProto.SetConfigMessage;
import org.flexiblepower.service.serializers.JavaIOSerializer;
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
public class ServiceManager {

    private static final Logger log = LoggerFactory.getLogger(ServiceManager.class);

    public static final byte[] SUCCESS = new byte[] {0};
    public static final byte[] FAILURE = new byte[] {1};
    public static final int MANAGEMENT_PORT = 4999;

    // private final Class<? extends Service> serviceClass;
    private boolean configured;
    private volatile boolean keepThreadAlive;
    private final Thread managerThread;

    private final ConnectionManager connectionManager;
    private final Socket managementSocket;
    private final Service service;
    private final JavaIOSerializer serializer = new JavaIOSerializer();

    ServiceManager(final Service service) {
        this.connectionManager = new ConnectionManager();
        this.service = service;
        this.managementSocket = ZMQ.context(1).socket(ZMQ.REP);
        this.managementSocket.setReceiveTimeOut(1000);

        // Because when this exists, it is initializing
        this.configured = false;
        this.keepThreadAlive = true;
        this.managerThread = new Thread(() -> {
            final String listenAddr = "tcp://*:" + ServiceManager.MANAGEMENT_PORT;
            ServiceManager.log.info("Start listening thread on {}", listenAddr);
            this.managementSocket.bind(listenAddr);

            while (this.keepThreadAlive) {
                try {
                    final byte[] data = this.managementSocket.recv();
                    if (data != null) {
                        this.managementSocket.send(this.handleServiceMessage(data));
                    }
                } catch (final ClosedSelectorException | ZError.IOException e) {
                    // Do nothing, we are stopped
                    ServiceManager.log.info("Socket closed, stopping thread");
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
     * Wait for the service management thread to stop
     *
     * @param force to enforce that the service thread will actually stop asap.
     */
    void join(final boolean force) {
        try {
            ServiceManager.log.info("Waiting for service thread to stop...");
            if (force) {
                this.keepThreadAlive = false;
                this.managerThread.interrupt();
                this.managementSocket.close();
                ServiceManager.log.debug("Force closed, now joining thread");
            }
            this.managerThread.join();
        } catch (final InterruptedException e) {
            ServiceManager.log.info("Interuption exception received, stopping...");
        }
    }

    void join() {
        this.join(false);
    }

    /**
     * @param data
     * @throws IOException
     * @throws ServiceInvocationException
     * @throws ConnectionModificationException
     */
    private byte[] handleServiceMessage(final byte[] data)
            throws IOException, ServiceInvocationException, ConnectionModificationException {
        try {
            final GoToProcessStateMessage msg = GoToProcessStateMessage.parseFrom(data);
            final Serializable response = this.handleGoToProcessStateMessage(msg);
            if (response != null) {
                return this.serializer.serialize(response);
            } else {
                return ServiceManager.SUCCESS;
            }
        } catch (final SerializationException e) {
            throw new ServiceInvocationException("Unable to serialize message response: " + e.getMessage(), e);
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
            this.connectionManager.handleConnectionMessage(msg);
            return ServiceManager.SUCCESS;
        } catch (final InvalidProtocolBufferException e) {
            // Not this type of message, try next
        }

        throw new InvalidProtocolBufferException("Invalid protobuf format");
    }

    /**
     * @param message
     * @throws ServiceInvocationException
     */
    private Serializable handleGoToProcessStateMessage(final GoToProcessStateMessage message)
            throws ServiceInvocationException {
        ServiceManager.log.info("Received GoToProcessStateMessage for process {} -> {}",
                message.getProcessId(),
                message.getTargetState());
        ServiceManager.log.trace("Received message: {}", message);

        switch (message.getTargetState()) {
        case RUNNING:
            // This is basically a "force start" with no configuration
            this.service.init(new Properties());
            this.configured = true;
            return null;

        case SUSPENDED:
            return this.service.suspend();

        case TERMINATED:
            this.service.terminate();
            this.connectionManager.close();
            this.keepThreadAlive = false;
            return null;

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
            final Serializable state = this.serializer.deserialize(msg.getStateData().toByteArray());
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
