/**
 * File ManagedConnection.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.flexiblepower.exceptions.SerializationException;
import org.flexiblepower.proto.ConnectionProto.ConnectionHandshake;
import org.flexiblepower.proto.ConnectionProto.ConnectionState;
import org.flexiblepower.serializers.MessageSerializer;
import org.flexiblepower.serializers.ProtobufMessageSerializer;
import org.flexiblepower.service.exceptions.ConnectionModificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQException;

import com.google.protobuf.Message;

import zmq.ZError;

/**
 * ManagedConnection
 *
 * @author coenvl
 * @version 0.1
 * @since May 12, 2017
 */
final class ManagedConnection implements Connection, Closeable {

    private static final Logger log = LoggerFactory.getLogger(ManagedConnection.class);
    private static final int RECEIVE_TIMEOUT = 100;
    private static final int MAX_HEARTBEAT_THREADS = 1;
    private static final long HEARTBEAT_PERIOD_IN_SECONDS = 60;
    private static final long INITIAL_HEARTBEAT_DELAY = 2;
    private static final int CONNECT_RETRY_DELAY_IN_SECONDS = 5;
    private static final byte[] PING = new byte[] {(byte) 0xA};
    private static final byte[] PONG = new byte[] {(byte) 0xB};
    private static final int HEARTBEAT_MSG_LENGTH = 1;

    private ConnectionState state;
    private volatile boolean keepThreadAlive;
    private final Context zmqContext;
    private final Socket subscribeSocket;
    private final Socket publishSocket;
    private final Thread connectionThread;
    private final ConnectionHandler handler;
    private final MessageSerializer<Object> userMessageSerializer;
    private final ProtobufMessageSerializer protoBufSerializer;
    private InterfaceInfo info = null;
    private final String connectionId;
    private boolean pinged;
    private ScheduledFuture<?> heartBeatThread;

    /**
     * @param targetAddress
     * @param listenPort
     * @throws ConnectionModificationException
     * @throws SerializationException
     *
     */
    @SuppressWarnings("unchecked")
    ManagedConnection(final String connectionId,
            final int listenPort,
            final String targetAddress,
            final ConnectionHandler handler) throws ConnectionModificationException {
        this.connectionId = connectionId;
        this.handler = handler;
        this.info = ManagedConnection.getInfoFromHandler(handler);

        // Add serializer to the connection for user-defined messages
        try {
            this.userMessageSerializer = this.info.serializer().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ConnectionModificationException("Unable to serializer instantiate connection");
        }

        for (final Class<?> messageType : this.info.receiveTypes()) {
            this.userMessageSerializer.addMessageClass(messageType);
        }

        // Add Protobuf serializer to connection for ConnectionHandshake messages
        this.protoBufSerializer = new ProtobufMessageSerializer();
        this.protoBufSerializer.addMessageClass(ConnectionHandshake.class);

        this.state = ConnectionState.STARTING;
        this.zmqContext = ZMQ.context(1);
        this.publishSocket = this.zmqContext.socket(ZMQ.PUSH);
        this.subscribeSocket = this.zmqContext.socket(ZMQ.PULL);

        ManagedConnection.log.debug("Creating publishSocket to {}", targetAddress);
        this.publishSocket.setSendTimeOut(0); // ManagedConnection.SEND_TIMEOUT);
        this.publishSocket.setDelayAttachOnConnect(true);

        this.keepThreadAlive = true;

        this.connectionThread = new Thread(() -> {
            this.tryToConnect(targetAddress);

            final String listenAddress = "tcp://*:" + listenPort;
            ManagedConnection.log.debug("Creating subscribeSocket listening on port {}", listenAddress);
            this.subscribeSocket.setReceiveTimeOut(ManagedConnection.RECEIVE_TIMEOUT);
            this.subscribeSocket.bind(listenAddress);

            final ConnectionHandshake initHandshakeMessage = ConnectionHandshake.newBuilder()
                    .setConnectionId(this.connectionId)
                    .setConnectionState(ConnectionState.STARTING)
                    .build();
            try {
                this.publishSocket.send(this.protoBufSerializer.serialize(initHandshakeMessage));
            } catch (final SerializationException e) {
                ManagedConnection.log.error("Error in serializing message: " + initHandshakeMessage);
            }

            this.heartBeatThread = new ScheduledThreadPoolExecutor(ManagedConnection.MAX_HEARTBEAT_THREADS)
                    .scheduleAtFixedRate(() -> {
                        if (!this.pinged) {
                            final byte[] heartbeat = ManagedConnection.PING;
                            this.pinged = true;
                            this.publishSocket.send(heartbeat);
                        } else {
                            // If no PONG was received since the last PING, assume connection was interrupted!
                            handler.onInterrupt();
                        }
                    },
                            ManagedConnection.INITIAL_HEARTBEAT_DELAY,
                            ManagedConnection.HEARTBEAT_PERIOD_IN_SECONDS,
                            TimeUnit.SECONDS);

            while (this.keepThreadAlive) {
                try {
                    this.handleByteArray(this.subscribeSocket.recv());
                } catch (final ZMQException e) {
                    if (e.getErrorCode() == 156384765) {
                        ManagedConnection.log.info("Socket closed, Attempting to reconnect");
                        this.tryToConnect(targetAddress);
                    }
                } catch (final Exception e) {
                    ManagedConnection.log.error("Exception handling message: {}", e.getMessage());
                    ManagedConnection.log.trace("Exception handing message", e);
                }
            }
            ManagedConnection.log.trace("End of thread");
            this.heartBeatThread.cancel(true);
        }, "Managed " + this.info.name() + " handler thread");
        this.connectionThread.start();
    }

    private void tryToConnect(final String targetAddress) {
        boolean connected = false;
        while (!connected) {
            try {
                this.publishSocket.connect(targetAddress);
                connected = true;
            } catch (final IllegalArgumentException e) {
                ManagedConnection.log.debug("Target" + targetAddress + " is still not up. Retrying in "
                        + ManagedConnection.CONNECT_RETRY_DELAY_IN_SECONDS + " seconds...");
                try {
                    Thread.sleep(ManagedConnection.CONNECT_RETRY_DELAY_IN_SECONDS * 1000);
                } catch (final InterruptedException e1) {
                    e1.printStackTrace();
                    break;
                }
            }
        }
    }

    /**
     * @param handler
     * @return
     * @throws ConnectionModificationException
     */
    private static InterfaceInfo getInfoFromHandler(final ConnectionHandler handler)
            throws ConnectionModificationException {
        if (handler.getClass().isAnnotationPresent(InterfaceInfo.class)) {
            return handler.getClass().getAnnotation(InterfaceInfo.class);
        } else {
            for (final Class<?> itf : handler.getClass().getInterfaces()) {
                if (itf.isAnnotationPresent(InterfaceInfo.class)) {
                    return itf.getAnnotation(InterfaceInfo.class);
                }
            }
        }
        throw new ConnectionModificationException("No interface information found on connection handler");
    }

    /**
     * @param buff
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws SerializationException
     */
    private void handleByteArray(final byte[] buff) throws IllegalAccessException,
            IllegalArgumentException,
            InvocationTargetException {
        if (buff == null) {
            return;
        }

        if (buff.length == ManagedConnection.HEARTBEAT_MSG_LENGTH) {
            // If message is only 1 byte long, it can only be a Heatbeat!
            if (buff.equals(ManagedConnection.PONG)) {
                // If ponged, it is a response to our ping
                this.pinged = false;
            } else {
                // If pinged, respond with a pong
                final byte[] response = ManagedConnection.PONG;
                this.publishSocket.send(response);
            }

        } else {
            // If message is longer than 1 byte, it can be a ConnectionHandshake or a user-defined process message!
            if (this.state.equals(ConnectionState.STARTING)) {
                // If connection state is STARTING, it can only be a Connection Handshake!
                Message receivedMsg = null;
                try {
                    receivedMsg = this.protoBufSerializer.deserialize(buff);
                } catch (final SerializationException e1) {
                    e1.printStackTrace();
                }
                final ConnectionHandshake handShakeMessage = (ConnectionHandshake) receivedMsg;
                if (handShakeMessage.getConnectionId().equals(this.connectionId)) {
                    ManagedConnection.log.debug("Received acknowledge string: {}", handShakeMessage);

                    this.state = ConnectionState.CONNECTED;
                    this.handler.onConnected(this);

                    ManagedConnection.log.debug("Updated state to {}, replying ack", this.state);

                    final ConnectionHandshake response = ConnectionHandshake.newBuilder()
                            .setConnectionId(this.connectionId)
                            .setConnectionState(ConnectionState.CONNECTED)
                            .build();

                    try {
                        this.publishSocket.send(this.protoBufSerializer.serialize(response));
                    } catch (final SerializationException e) {
                        ManagedConnection.log.error("Error in serializing " + response);
                    }
                } else {
                    ManagedConnection.log
                            .warn("Invalid Connection ID in Handshake message : " + handShakeMessage.getConnectionId());
                }
            } else {
                // If connection state is CONNECTED, it can only be a user-defined process message!
                try {
                    final Object message = this.userMessageSerializer.deserialize(buff);
                    final Class<?> messageType = message.getClass();
                    final Method[] allMethods = this.handler.getClass().getMethods();
                    for (final Method method : allMethods) {
                        if ((method.getName().startsWith("handle")) && (method.getName().endsWith("Message"))
                                && (method.getParameterCount() == 1)
                                && method.getParameterTypes()[0].equals(messageType)) {
                            method.invoke(this.handler, message);
                        }
                    }
                } catch (final SerializationException e) {
                    // Not a user-defined message, so ignore with grace!
                    ManagedConnection.log.warn("Received unknown message : " + new String(buff) + ". Ignoring...");
                }
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
                this.publishSocket.send(this.userMessageSerializer.serialize(message));
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
        final ConnectionState previous = this.state;
        this.state = ConnectionState.CONNECTED;
        if (previous.equals(ConnectionState.SUSPENDED)) {
            this.handler.resumeAfterSuspend();
        } else if (previous.equals(ConnectionState.INTERRUPTED)) {
            this.handler.resumeAfterInterrupt();
        }
    }

    public void suspend() {
        this.state = ConnectionState.SUSPENDED;
        this.handler.onSuspend();
    }

    @Override
    public void close() {
        this.keepThreadAlive = false;
        this.state = ConnectionState.TERMINATED;
        this.handler.terminated();

        try {
            this.connectionThread.join();
        } catch (final InterruptedException e) {
            // Do nothing
        }

        if (this.publishSocket != null) {
            this.publishSocket.close();
        }

        if (this.subscribeSocket != null) {
            this.subscribeSocket.close();
        }

        if (this.zmqContext != null) {
            try {
                this.zmqContext.close();
            } catch (final ZError.IOException e) {
                // Do nothing
                // This happens apparently if the socket is closed while someone is polling it.
            }
        }
    }

}
