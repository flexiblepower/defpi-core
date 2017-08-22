/**
 * File ManagedConnection.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.ClosedSelectorException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
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

/**
 * ManagedConnection
 *
 * @version 0.1
 * @since May 12, 2017
 */
final class ManagedConnection implements Connection {

    private static final Logger log = LoggerFactory.getLogger(ManagedConnection.class);
    private static final int RECEIVE_TIMEOUT = 100;
    private static final int MAX_HEARTBEAT_THREADS = 1;
    private static final long HEARTBEAT_PERIOD_IN_SECONDS = 10;
    private static final long INITIAL_HEARTBEAT_DELAY = 2;
    private static final long MAX_BACKOFF_MS = 60000;
    private static final byte[] PING = new byte[] {(byte) 0xA};
    private static final byte[] PONG = new byte[] {(byte) 0xB};
    private static final int HEARTBEAT_MSG_LENGTH = 1;

    private volatile ConnectionState state;
    private final Context zmqContext;
    private Socket subscribeSocket;
    private Socket publishSocket;
    private Thread connectionThread;
    private final ConnectionHandler handler;
    private final MessageSerializer<Object> userMessageSerializer;
    private final ProtobufMessageSerializer connectionHandshakeSerializer;
    private InterfaceInfo info = null;
    private final String connectionId;
    private boolean pinged;
    private ScheduledFuture<?> heartBeatFuture;
    private final ExecutorService serviceExecutor;
    private final Object suspendLock = new Object();

    private int listenPort;
    private String targetAddress;
    private final ScheduledThreadPoolExecutor heartBeatExecutor;

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
            final ConnectionHandler handler,
            final ExecutorService serviceExecutor) throws ConnectionModificationException {
        this.connectionId = connectionId;
        this.handler = handler;
        this.serviceExecutor = serviceExecutor;
        this.info = ManagedConnection.getInfoFromHandler(handler);
        this.listenPort = listenPort;
        this.targetAddress = targetAddress;

        this.heartBeatExecutor = new ScheduledThreadPoolExecutor(ManagedConnection.MAX_HEARTBEAT_THREADS);

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
        this.connectionHandshakeSerializer = new ProtobufMessageSerializer();
        this.connectionHandshakeSerializer.addMessageClass(ConnectionHandshake.class);

        // Init ZMQ
        this.zmqContext = ZMQ.context(1);

        // Start main loop
        this.startMainLoop();
    }

    private void startMainLoop() {
        this.state = ConnectionState.STARTING;

        this.initListening();

        this.connectionThread = new Thread(() -> {
            long backOffMs = 100;
            while (this.state != ConnectionState.TERMINATED) {

                if (this.state == ConnectionState.STARTING) {
                    // State is STARTING, goal is to connect
                    final boolean success = this.tryConnectSending();
                    if (success) {
                        // Update state
                        this.state = ConnectionState.CONNECTED;

                        // reset backOff
                        backOffMs = 100;

                        // Notify Service implementation
                        this.serviceExecutor.submit(() -> {
                            try {
                                this.handler.onConnected(this);
                            } catch (final Throwable e) {
                                ManagedConnection.log.error("Error while calling onConnected(Connection)", e);
                            }
                        });
                    } else {
                        backOffMs = Math.min(ManagedConnection.MAX_BACKOFF_MS, backOffMs * 2);
                        try {
                            Thread.sleep(backOffMs);
                        } catch (final InterruptedException e) {
                            // Don't care, we'll see you next iteration
                        }
                    }
                } else if (this.state == ConnectionState.CONNECTED) {
                    // State is CONNECTED, goal is to handle messages
                    this.tryReceiveMessage();

                } else if (this.state == ConnectionState.SUSPENDED) {
                    // State is SUSPENDED, there are two options: try to reconnect or wait for instructions to reconnect
                    if (this.listenPort == 0) {
                        // We are suspended and have received no instruction to resume, wait for instruction
                        synchronized (this.suspendLock) {
                            try {
                                this.suspendLock.wait();
                            } catch (final InterruptedException e) {
                                // Don't care, we'll see you next iteration
                            }
                        }
                    } else {
                        // We are suspended, but we want to go back to CONNECTED, try to connect
                        final boolean success = this.tryConnectSending();
                        if (success) {
                            // Update state
                            this.state = ConnectionState.CONNECTED;

                            // reset backOff
                            backOffMs = 100;

                            // Notify Service implementation
                            this.serviceExecutor.submit(() -> {
                                try {
                                    this.handler.resumeAfterSuspend();
                                } catch (final Throwable e) {
                                    ManagedConnection.log.error("Error while calling resumeAfterSuspend()", e);
                                }
                            });
                        } else {
                            backOffMs = Math.min(ManagedConnection.MAX_BACKOFF_MS, backOffMs * 2);
                            try {
                                Thread.sleep(backOffMs);
                            } catch (final InterruptedException e) {
                                // Don't care, we'll see you next iteration
                            }
                        }
                    }

                } else if (this.state == ConnectionState.INTERRUPTED) {
                    // State is INTERRUPTED, we have to try to reconnect
                    final boolean success = this.tryConnectSending();
                    if (success) {
                        // reset backOff
                        backOffMs = 100;

                        this.resumeAfterInterruptedState();
                    } else {
                        backOffMs = Math.min(ManagedConnection.MAX_BACKOFF_MS, backOffMs * 2);
                        try {
                            Thread.sleep(backOffMs);
                        } catch (final InterruptedException e) {
                            // Don't care, we'll see you next iteration
                        }
                    }
                }
            }

            // State is TERMINATED, cleanup
            this.disconnectListening();
            this.disconnectSending();
            this.zmqContext.close();
            this.heartBeatExecutor.shutdownNow();
        });
        this.connectionThread.start();
    }

    private void initListening() {
        if (this.subscribeSocket == null) {
            this.subscribeSocket = this.zmqContext.socket(ZMQ.PULL);
            final String listenAddress = "tcp://*:" + this.listenPort;
            ManagedConnection.log.debug("Creating subscribeSocket listening on port {}", listenAddress);
            this.subscribeSocket.setReceiveTimeOut(ManagedConnection.RECEIVE_TIMEOUT);
            this.subscribeSocket.bind(listenAddress);
        } else {
            ManagedConnection.log.debug("Not re-creating subscribesocket");
        }
    }

    private void disconnectListening() {
        if (this.subscribeSocket != null) {
            this.subscribeSocket.close();
            this.subscribeSocket = null;
        }
    }

    private boolean tryConnectSending() {
        // Initialize socket
        if (this.publishSocket == null) {
            this.publishSocket = this.zmqContext.socket(ZMQ.PUSH);
            this.publishSocket.setSendTimeOut(0); // ManagedConnection.SEND_TIMEOUT);
            this.publishSocket.setDelayAttachOnConnect(true);
        }

        // Try to connect
        try {
            this.publishSocket.connect(this.targetAddress);
        } catch (final IllegalArgumentException e) {
            // Could not resolve hostname, other container is not yet ready
            return false;
        }

        // Send the handshake
        final ConnectionHandshake initHandshakeMessage = ConnectionHandshake.newBuilder()
                .setConnectionId(this.connectionId)
                .setConnectionState(this.state)
                .build();
        try {
            if (!this.publishSocket.send(this.connectionHandshakeSerializer.serialize(initHandshakeMessage))) {
                // Failed sending handshake
                return false;
            }
        } catch (final SerializationException e) {
            // This should not happen
            ManagedConnection.log.error("Error in serializing message: " + initHandshakeMessage);
            return false;
        }

        // Receive the HandShake
        for (int i = 0; (this.state != ConnectionState.TERMINATED) && (i < 100); i++) {
            byte[] data = null;
            try {
                data = this.subscribeSocket.recv();
            } catch (final Exception e) {
                // The subscribeSocket is closed, probably the session was suspended before it was running
                return false;
            }
            if (data == null) {
                // That can happen, try again
                continue;
            }
            if (data.length == ManagedConnection.HEARTBEAT_MSG_LENGTH) {
                // Whoops, that's a heart beat, try again
                continue;
            }
            Message receivedMsg;
            try {
                receivedMsg = this.connectionHandshakeSerializer.deserialize(data);
            } catch (final SerializationException e) {
                ManagedConnection.log.error("Could not deserialize conenctionhandshake message", e);
                continue;
            }
            final ConnectionHandshake handShakeMessage = (ConnectionHandshake) receivedMsg;

            if (handShakeMessage.getConnectionId().equals(this.connectionId)) {
                ManagedConnection.log.debug("Received acknowledge string: {}", handShakeMessage);
                // Success! Prepare for real communication.
                this.startHeartBeat();
                return true;
            } else {
                ManagedConnection.log
                        .warn("Invalid Connection ID in Handshake message : " + handShakeMessage.getConnectionId());
                continue;
            }
        }
        // Still no luck
        return false;
    }

    private void disconnectSending() {
        if (this.publishSocket != null) {
            this.publishSocket.close();
            this.publishSocket = null;
        }
    }

    /**
     * Go to the INTERRUPTED state. This method is called when sending fails or when no heartbeats are received.
     */
    private void goToInterruptedState() {
        if (this.state == ConnectionState.INTERRUPTED) {
            return;
        }

        // Update state
        this.state = ConnectionState.INTERRUPTED;

        // Stop heartbeat
        this.stopHeartBeat();

        // Notify Service implementation
        this.serviceExecutor.submit(() -> {
            try {
                this.handler.onInterrupt();
            } catch (final Throwable e) {
                ManagedConnection.log.error("Error while calling onInterrupt()", e);
            }
        });
    }

    private void resumeAfterInterruptedState() {
        // Update state
        this.state = ConnectionState.CONNECTED;

        // Notify Service implementation
        this.serviceExecutor.submit(() -> {
            try {
                this.handler.resumeAfterInterrupt();
            } catch (final Throwable e) {
                ManagedConnection.log.error("Error while calling resumeAfterInterrupt()", e);
            }
        });
    }

    private void tryReceiveMessage() {
        byte[] buff = null;
        try {
            buff = this.subscribeSocket.recv();
        } catch (final ZMQException e) {
            if (e.getErrorCode() == 156384765) {
                ManagedConnection.log.info("Socket closed, Attempting to reconnect");
                this.goToInterruptedState();
                return;
            }
        } catch (final ClosedSelectorException | AssertionError e) {
            // The connection was suspended or terimanted
            return;
        }
        if (buff == null) {
            return;
        }

        if (buff.length == ManagedConnection.HEARTBEAT_MSG_LENGTH) {
            // If message is only 1 byte long, it can only be a Heatbeat!
            if (Arrays.equals(buff, ManagedConnection.PONG)) {
                // If ponged, it is a response to our ping
                this.pinged = false;
            } else if (Arrays.equals(buff, ManagedConnection.PING)) {
                // If pinged, respond with a pong
                final byte[] response = ManagedConnection.PONG;
                this.publishSocket.send(response);
            }

        } else {
            // It can only be a user-defined process message!
            try {
                final Object message = this.userMessageSerializer.deserialize(buff);
                final Class<?> messageType = message.getClass();
                final Method[] allMethods = this.handler.getClass().getMethods();
                for (final Method method : allMethods) {
                    if ((method.getName().startsWith("handle")) && (method.getName().endsWith("Message"))
                            && (method.getParameterCount() == 1) && method.getParameterTypes()[0].equals(messageType)) {
                        this.serviceExecutor.submit(() -> {
                            try {
                                method.invoke(this.handler, message);
                            } catch (IllegalAccessException | IllegalArgumentException e) {
                                ManagedConnection.log.error("Message handling method is not properly formatted", e);
                            } catch (final InvocationTargetException e) {
                                ManagedConnection.log.error("Exception while invoking " + method.getName() + "("
                                        + messageType.getSimpleName() + ") method", e.getTargetException());
                            }
                        });
                    }
                }
            } catch (final SerializationException e) {
                // Not a user-defined message, so ignore with grace!
                ManagedConnection.log.warn("Received unknown message : " + new String(buff) + ". Ignoring...");
            }
        }
    }

    /**
     * Try to go back from SUSPENDED to CONNECTED. This message prepares the main loop to try to connect again.
     *
     * @param listenPort
     * @param targetAddress
     */
    public void resumeAfterSuspendedState(final int listenPort, final String targetAddress) {
        // Make sure we know that we want to go from SUSPENDED to RUNNING
        this.listenPort = listenPort;
        this.targetAddress = targetAddress;

        // Make sure we are ready to listen
        this.initListening();

        // Wake up the main loop, so it tries to connect the sending
        synchronized (this.suspendLock) {
            this.suspendLock.notifyAll();
        }
    }

    public void goToTerminatedState() {
        // Notify Service implementation
        this.serviceExecutor.submit(() -> {
            try {
                this.handler.terminated();
            } catch (final Throwable e) {
                ManagedConnection.log.error("Error while calling onSuspend()", e);
            }
        });

        // Update the state
        this.state = ConnectionState.TERMINATED;
    }

    /**
     * Go to the SUSPENDED State. In the SUSPENDED state all communication is stopped (both listening and receiving).
     * The connection is waiting until it receives instruction to reconnect.
     */
    public void goToSuspendedState() {
        // Notify Service implementation
        this.serviceExecutor.submit(() -> {
            try {
                this.handler.onSuspend();
            } catch (final Throwable e) {
                ManagedConnection.log.error("Error while calling onSuspend()", e);
            }
        });

        // Stop communication
        this.disconnectListening();
        this.disconnectSending();

        // Indicate that we have received no instruction to resume
        this.listenPort = 0;
        this.targetAddress = null;

        // Update the state
        this.state = ConnectionState.SUSPENDED;
    }

    private void startHeartBeat() {
        if (this.heartBeatFuture != null) {
            this.heartBeatFuture.cancel(true);
        }
        this.pinged = false;
        this.heartBeatFuture = this.heartBeatExecutor.scheduleAtFixedRate(() -> {
            if (!this.pinged) {
                this.pinged = true;
                this.publishSocket.send(ManagedConnection.PING);
            } else {
                // If no PONG was received since the last PING, assume connection was interrupted!
                ManagedConnection.log.warn("No heartbeat received on connection, goto {}", ConnectionState.INTERRUPTED);
                this.goToInterruptedState();
            }
        }, ManagedConnection.INITIAL_HEARTBEAT_DELAY, ManagedConnection.HEARTBEAT_PERIOD_IN_SECONDS, TimeUnit.SECONDS);
    }

    private void stopHeartBeat() {
        this.heartBeatFuture.cancel(true);
        this.heartBeatFuture = null;
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

        boolean valid = false;
        for (final Class<?> c : this.info.sendTypes()) {
            if (c.isInstance(message)) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            throw new IllegalArgumentException(
                    "The message type '" + message.getClass().getSimpleName() + "' is not defined in the interface");
        }

        if (this.getState().equals(ConnectionState.CONNECTED)) {
            boolean success = false;
            try {
                // Do the send
                success = this.publishSocket.send(this.userMessageSerializer.serialize(message));
            } catch (final SerializationException e) {
                throw new IllegalArgumentException(e);
            } catch (final Exception e) {
                success = false;
            }
            if (!success) {
                ManagedConnection.log.warn("Failed to send message through socket, goto {}",
                        ConnectionState.INTERRUPTED);
                this.goToInterruptedState();
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

    public void waitTillFinished() throws InterruptedException {
        if (this.connectionThread.isAlive()) {
            this.connectionThread.join();
        }
    }

}
