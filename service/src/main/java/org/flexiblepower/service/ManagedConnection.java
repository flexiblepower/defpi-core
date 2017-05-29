/**
 * File ManagedConnection.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.flexiblepower.service.exceptions.ConnectionModificationException;
import org.flexiblepower.service.exceptions.SerializationException;
import org.flexiblepower.service.serializers.MessageSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQException;

/**
 * ManagedConnection
 *
 * @author coenvl
 * @version 0.1
 * @since May 12, 2017
 */
final class ManagedConnection implements Connection {

    private static final Logger log = LoggerFactory.getLogger(ManagedConnection.class);

    private static final String ACK_PREFIX = "@Defpi-0.2.1 connection ready";
    private static final int SEND_TIMEOUT = 2000;
    private static final int RECEIVE_TIMEOUT = 1000;

    private ConnectionState state;
    private volatile boolean keepThreadAlive;

    private final Context zmqContext;
    private final Socket subscribeSocket;
    private final Socket publishSocket;

    private final ConnectionHandler handler;
    private final MessageSerializer<Object> serializer;

    private InterfaceInfo info = null;

    /**
     * @param targetAddress
     * @param listenPort
     * @throws ConnectionModificationException
     *
     */
    ManagedConnection(final int listenPort, final String targetAddress, final ConnectionHandler handler)
            throws ConnectionModificationException {
        this.handler = handler;

        if (handler.getClass().isAnnotationPresent(InterfaceInfo.class)) {
            this.info = handler.getClass().getAnnotation(InterfaceInfo.class);
        } else {
            for (final Class<?> itf : handler.getClass().getInterfaces()) {
                if (itf.isAnnotationPresent(InterfaceInfo.class)) {
                    this.info = itf.getAnnotation(InterfaceInfo.class);
                    break;
                }
            }
        }

        if (this.info == null) {
            throw new ConnectionModificationException("No interface information found on connection handler");
        }

        // Add serializer to the connection
        try {
            this.serializer = (MessageSerializer<Object>) this.info.serializer().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ConnectionModificationException("Unable to serializer instantiate connection");
        }

        for (final Class<?> messageType : this.info.receiveTypes()) {
            this.serializer.addMessageClass(messageType);
        }

        this.state = ConnectionState.STARTING;
        this.zmqContext = ZMQ.context(1);

        ManagedConnection.log.debug("Creating publishSocket to {}", targetAddress);
        this.publishSocket = this.zmqContext.socket(ZMQ.PUSH);
        this.publishSocket.setSendTimeOut(ManagedConnection.SEND_TIMEOUT);
        this.publishSocket.bindToRandomPort("tcp://*");
        this.publishSocket.setDelayAttachOnConnect(true);
        this.publishSocket.connect(targetAddress);

        final String listenAddress = "tcp://*:" + listenPort;
        ManagedConnection.log.debug("Creating subscribeSocket listening on port {}", listenAddress);
        this.subscribeSocket = this.zmqContext.socket(ZMQ.PULL);
        this.subscribeSocket.setReceiveTimeOut(ManagedConnection.RECEIVE_TIMEOUT);
        this.subscribeSocket.bind(listenAddress);
        // this.subscribeSocket.subscribe("".getBytes());
        this.keepThreadAlive = true;

        if (this.publishSocket.send(this.acknowledge())) {
            ManagedConnection.log.debug("Succesfully sent acknowledge");
        } else {
            ManagedConnection.log.debug("Failed to send acknowledge");
        }

        final Thread t = new Thread(() -> {
            while (this.keepThreadAlive) {
                try {
                    this.handleByteArray(this.subscribeSocket.recv());
                } catch (final ZMQException e) {
                    if (e.getErrorCode() == 156384765) {
                        ManagedConnection.log.info("Socket closed, stopping thread");
                        break;
                    }
                } catch (final Exception e) {
                    ManagedConnection.log.error("Exception handling message: {}", e.getMessage());
                    ManagedConnection.log.trace("Exception handing message", e);
                }
            }
            ManagedConnection.log.trace("End of thread");
        }, "Managed " + this.info.name() + " handler thread");

        t.start();
    }

    /**
     * @return a special series of bytes that indicate this connection is ready.
     */
    private byte[] acknowledge() {
        final String ack = String.format("%s:%s/%s@%s",
                ManagedConnection.ACK_PREFIX,
                this.info.receivesHash(),
                this.info.sendsHash(),
                this.info.serializer());
        return ack.getBytes();
    }

    private boolean handleAck(final byte[] barr) {
        final String test = new String(barr);

        final String expected = String.format("%s:%s/%s@%s",
                ManagedConnection.ACK_PREFIX,
                this.info.sendsHash(),
                this.info.receivesHash(),
                this.info.serializer());

        if (test.startsWith(ManagedConnection.ACK_PREFIX)) {
            ManagedConnection.log.debug("Received acknowledge string: {}", test);
            if (test.equals(expected)) {
                this.state = ConnectionState.CONNECTED;
                ManagedConnection.log.debug("Updated state to {}, replying ack", this.state);
                this.publishSocket.send(this.acknowledge());
                return true;
            } else {
                ManagedConnection.log.warn("Unexpected ACK");
            }
        }
        return false;
    }

    /**
     * @param buff
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws SerializationException
     */
    private void handleByteArray(final byte[] buff)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, SerializationException {
        if (buff == null) {
            return;
        }

        if ((this.state == ConnectionState.STARTING) && this.handleAck(buff)) {
            return;
        }

        final Object message = this.serializer.deserialize(buff);

        final Class<?> messageType = message.getClass();
        final Method[] allMethods = this.handler.getClass().getMethods();
        for (final Method method : allMethods) {
            if ((method.getParameterCount() == 1) && method.getParameterTypes()[0].equals(messageType)) {
                method.invoke(this.handler, message);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.Connection#send(java.lang.Object)
     */
    @Override
    public void send(final Object message) {
        if (message == null) {
            return;
        }

        if (this.getState().equals(ConnectionState.CONNECTED)) {
            try {
                // Do the send
                this.publishSocket.send(this.serializer.serialize(message));
            } catch (final Exception e) {
                this.state = ConnectionState.INTERRUPTED;
                // TODO Recover from the Interrupted state (or via resume?)
            }
        } else {
            ManagedConnection.log.warn("Unable to send when connection state is {}!", this.state);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.Connection#getState()
     */
    @Override
    public ConnectionState getState() {
        return this.state;
    }

    public void resume() {
        // if (this.state.equals(ConnectionState.SUSPENDED)) {
        this.state = ConnectionState.CONNECTED;
        // }
    }

    public void suspend() {
        this.state = ConnectionState.SUSPENDED;
    }

    void close() {
        this.keepThreadAlive = false;
        this.state = ConnectionState.TERMINATED;

        if (this.publishSocket != null) {
            this.publishSocket.close();
        }

        if (this.subscribeSocket != null) {
            this.subscribeSocket.close();
        }

        if (this.zmqContext != null) {
            this.zmqContext.close();
        }
    }

}
