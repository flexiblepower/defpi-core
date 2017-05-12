/**
 * File ManagedConnection.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.util.HashSet;
import java.util.Set;

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

    private final Set<MessageHandlerWrapper> handlers;

    private final Context zmqContext;
    private final Socket subscribeSocket;
    private final Socket publishSocket;

    /**
     * @param targetAddress
     * @param listenPort
     *
     */
    ManagedConnection(final int listenPort, final String targetAddress) {
        this.state = ConnectionState.STARTING;
        this.handlers = new HashSet<>();
        this.zmqContext = ZMQ.context(1);

        this.publishSocket = this.zmqContext.socket(ZMQ.PUB);
        this.publishSocket.setSendTimeOut(ManagedConnection.REQUEST_TIMEOUT);
        this.publishSocket.connect(targetAddress);

        this.subscribeSocket = this.zmqContext.socket(ZMQ.SUB);
        this.subscribeSocket.bind("tcp://*:" + listenPort);
        this.subscribeSocket.subscribe("".getBytes());

        final Thread t = new Thread(() -> {
            final byte[] buff = this.subscribeSocket.recv(0);
            for (final MessageHandlerWrapper handler : this.handlers) {
                try {
                    this.send(handler.handleMessage(buff));
                } catch (final Exception e) {
                    // Not the right type of handler... try next
                    continue;
                }
            }
        });
        t.start();
    }

    void addHandler(final MessageHandlerWrapper handler) {
        this.handlers.add(handler);
        handler.onConnected(this);
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
