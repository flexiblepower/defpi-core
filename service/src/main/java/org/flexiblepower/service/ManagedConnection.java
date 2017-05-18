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
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

/**
 * ManagedConnection
 *
 * @author coenvl
 * @version 0.1
 * @since May 12, 2017
 */
final class ManagedConnection implements Connection {

    private static final int REQUEST_TIMEOUT = 2000;

    private ConnectionState state;

    private final Context zmqContext;
    private final Socket subscribeSocket;
    private final Socket publishSocket;

    private final ConnectionHandler handler;
    private final MessageSerializer<?> serializer;

    /**
     * @param targetAddress
     * @param listenPort
     * @throws ConnectionModificationException
     *
     */
    ManagedConnection(final int listenPort, final String targetAddress, final ConnectionHandler handler)
            throws ConnectionModificationException {
        this.handler = handler;
        InterfaceInfo info = null;
        for (final Class<?> itf : handler.getClass().getInterfaces()) {
            if (itf.isAnnotationPresent(InterfaceInfo.class)) {
                info = itf.getAnnotation(InterfaceInfo.class);
                break;
            }
        }
        if (info == null) {
            throw new ConnectionModificationException("No interface information found on connection handler");
        }

        try {
            this.serializer = info.receiveSerializer().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ConnectionModificationException("Unable to serializer instantiate connection");
        }

        for (final Class<?> messageType : info.receiveTypes()) {
            this.serializer.addMessageClass(messageType);
        }

        this.state = ConnectionState.STARTING;
        this.zmqContext = ZMQ.context(1);

        this.publishSocket = this.zmqContext.socket(ZMQ.PUB);
        this.publishSocket.setSendTimeOut(ManagedConnection.REQUEST_TIMEOUT);
        this.publishSocket.connect(targetAddress);

        this.subscribeSocket = this.zmqContext.socket(ZMQ.SUB);
        this.subscribeSocket.bind("tcp://*:" + listenPort);
        this.subscribeSocket.subscribe("".getBytes());

        final Thread t = new Thread(() -> {
            final byte[] buff = this.subscribeSocket.recv(0);
            // for (final ConnectionHandler handler : this.handlers) {
            this.handleByteArray(buff);
            // }
        });
        t.start();
    }

    /**
     * @param buff
     */
    private void handleByteArray(final byte[] buff) {
        try {
            final Object message = this.serializer.deserialize(buff);

            final Class<?> messageType = message.getClass();
            final Method[] allMethods = this.handler.getClass().getMethods();
            for (final Method method : allMethods) {
                if ((method.getParameterCount() == 1) && method.getParameterTypes()[0].equals(messageType)) {
                    method.invoke(message);
                }
            }

        } catch (final SerializationException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.Connection#send(java.lang.Object)
     */
    @Override
    public void send(final byte[] message) {
        if (message == null) {
            return;
        }

        if (this.getState().equals(ConnectionState.CONNECTED)) {
            try {
                // Do the send
                this.publishSocket.send(message);
            } catch (final Exception e) {
                this.state = ConnectionState.INTERRUPTED;
                // TODO Recover from the Interrupted state (or via resume?)
            }
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
