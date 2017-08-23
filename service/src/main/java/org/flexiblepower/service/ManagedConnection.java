/**
 * File ManagedConnection.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.ClosedSelectorException;
import java.util.concurrent.ExecutorService;

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
final class ManagedConnection implements Connection, Closeable {

    private static final Logger log = LoggerFactory.getLogger(ManagedConnection.class);
    private static final int RECEIVE_TIMEOUT = 100;
    private static final int SEND_TIMEOUT = 200;

    private static final long MAX_BACKOFF_MS = 60000;

    private volatile ConnectionState state;
    private final Context zmqContext;
    private Socket subscribeSocket;
    private Socket publishSocket;
    private Thread connectionThread;
    private ConnectionHandler handler;
    private MessageSerializer<Object> userMessageSerializer;
    private final ProtobufMessageSerializer connectionHandshakeSerializer;
    private final InterfaceInfo info;
    private final String connectionId;

    private final ExecutorService serviceExecutor;
    private final Object suspendLock = new Object();

    private int listenPort;
    private String targetAddress;

    private final HeartBeatMonitor heartBeat;

    /**
     * @param targetAddress
     * @param listenPort
     * @param info2
     * @throws ConnectionModificationException
     * @throws SerializationException
     *
     */
    ManagedConnection(final String connectionId,
            final int listenPort,
            final String targetAddress,
            final InterfaceInfo info) throws ConnectionModificationException {
        this.connectionId = connectionId;
        this.listenPort = listenPort;
        this.targetAddress = targetAddress;
        this.info = info;

        this.serviceExecutor = ServiceManager.getServiceExecutor();
        this.heartBeat = new HeartBeatMonitor(this);
        this.state = ConnectionState.STARTING;

        // Add Protobuf serializer to connection for ConnectionHandshake messages
        this.connectionHandshakeSerializer = new ProtobufMessageSerializer();
        this.connectionHandshakeSerializer.addMessageClass(ConnectionHandshake.class);

        // Add serializer to the connection for user-defined messages
        try {
            this.userMessageSerializer = this.info.serializer().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            ManagedConnection.log.error("Unable to serializer instantiate connection");
            throw new RuntimeException("Unable to serializer instantiate connection");
        }

        for (final Class<?> messageType : this.info.receiveTypes()) {
            this.userMessageSerializer.addMessageClass(messageType);
        }

        // Init ZMQ
        this.zmqContext = ZMQ.context(1);

        // Start main loop
        this.startMainLoop();
    }

    private void startMainLoop() {
        this.state = ConnectionState.STARTING;

        this.initListening();

        this.connectionThread = new Thread(new ConnectionRunner());
        this.connectionThread.start();
    }

    private void initListening() {
        if (this.subscribeSocket != null) {
            ManagedConnection.log.debug("Re-creating subscribesocket");
            this.closeSubscribeSocket();
            try {
                Thread.sleep(100);
            } catch (final InterruptedException e) {
                // Do nothing
            }
        }

        this.subscribeSocket = this.zmqContext.socket(ZMQ.PULL);
        final String listenAddress = "tcp://*:" + this.listenPort;
        ManagedConnection.log.debug("Creating subscribeSocket listening on port {}", listenAddress);
        this.subscribeSocket.setReceiveTimeOut(ManagedConnection.RECEIVE_TIMEOUT);
        this.subscribeSocket.bind(listenAddress);
    }

    /**
    *
    */
    protected void initConnectionHandler() {
        this.handler = ConnectionManager.buildHandlerForConnection(this, this.info);
    }

    private boolean tryConnectSending() {
        // Initialize socket
        if (this.publishSocket == null) {
            this.publishSocket = this.zmqContext.socket(ZMQ.PUSH);
            this.publishSocket.setSendTimeOut(ManagedConnection.SEND_TIMEOUT);
            this.publishSocket.setImmediate(false);
        }

        // Try to connect
        try {
            ManagedConnection.log.debug("Creating publishSocket sending to {}", this.targetAddress);
            if (!this.publishSocket.connect(this.targetAddress)) {
                ManagedConnection.log.debug("Failed to connect to {}, remote side not ready?");
                return false;
            }
        } catch (final IllegalArgumentException e) {
            // Could not resolve hostname, other container is not yet ready
            ManagedConnection.log.debug("Exception while connecting to remote: {}", e.getMessage());
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
                ManagedConnection.log.warn("Failed to send handshake");
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
                ManagedConnection.log.debug("Listening for handshake..");
                data = this.subscribeSocket.recv();
            } catch (final Exception e) {
                // The subscribeSocket is closed, probably the session was suspended before it was running
                ManagedConnection.log.warn("Exception while receiving from socket: {}", e.getMessage());
                return false;
            }
            if (data == null) {
                // That can happen, try again
                ManagedConnection.log.debug("No handshake received, retrying...");
                continue;
            }

            if (this.heartBeat.handleMessage(data)) {
                // Whoops, that's a heart beat, try again
                ManagedConnection.log.debug("Received heartbeat {} instead of handshake, retrying...",
                        new String(data));
                continue;
            }

            Message receivedMsg;
            try {
                receivedMsg = this.connectionHandshakeSerializer.deserialize(data);
            } catch (final SerializationException e) {
                ManagedConnection.log.error("Could not deserialize connectionhandshake message", e);
                continue;
            }
            final ConnectionHandshake handShakeMessage = (ConnectionHandshake) receivedMsg;

            if (handShakeMessage.getConnectionId().equals(this.connectionId)) {
                ManagedConnection.log.debug("Received acknowledge string: {}", handShakeMessage);
                // Success! Prepare for real communication.
                this.heartBeat.start();
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

    private void closePublishSocket() {
        if (this.publishSocket != null) {
            this.publishSocket.close();
            this.publishSocket = null;
        }
    }

    /**
     *
     */
    private void closeSubscribeSocket() {
        if (this.subscribeSocket != null) {
            this.subscribeSocket.close();
            this.subscribeSocket = null;
        }
    }

    @Override
    public synchronized void close() {
        ManagedConnection.log.debug("Closing connection...");
        this.state = ConnectionState.TERMINATED;
        this.closePublishSocket();
        this.closeSubscribeSocket();

        this.heartBeat.close();

        if (this.zmqContext != null) {
            this.zmqContext.close();
        }
        ManagedConnection.log.debug("Connection closed");
    }

    /**
     * Go to the INTERRUPTED state. This method is called when sending fails or when no heartbeats are received.
     */
    void goToInterruptedState() {
        if (this.state == ConnectionState.INTERRUPTED) {
            return;
        }

        // Update state
        this.state = ConnectionState.INTERRUPTED;

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
                if (this.state == ConnectionState.CONNECTED) {
                    ManagedConnection.log.warn("Receive socket was disconnected.");
                    this.goToInterruptedState();
                }
                return;
            }
        } catch (final ClosedSelectorException | AssertionError e) {
            // The connection was suspended or terminated
            return;
        }
        if (buff == null) {
            return;
        }

        if (this.heartBeat.handleMessage(buff)) {
            return;
        }

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

    /**
     * Try to go back from SUSPENDED to CONNECTED. This message prepares the main loop to try to connect again.
     *
     * @param listenPort
     * @param targetAddress
     */
    public void resumeAfterSuspendedState(final int newListenPort, final String newTargetAddress) {
        // Make sure we know that we want to go from SUSPENDED to RUNNING
        this.listenPort = newListenPort;
        this.targetAddress = newTargetAddress;

        // Make sure we are ready to listen
        this.initListening();

        // Wake up the main loop, so it tries to connect the sending
        synchronized (this.suspendLock) {
            this.suspendLock.notifyAll();
        }
    }

    public void goToTerminatedState() {
        // Update the state
        this.state = ConnectionState.TERMINATED;

        if (this.handler == null) {
            // Handshake was not even finished...
            return;
        }

        // Notify Service implementation
        this.serviceExecutor.submit(() -> {
            try {
                this.handler.terminated();
            } catch (final Throwable e) {
                ManagedConnection.log.error("Error while calling terminated()", e);
            }
        });

    }

    /**
     * Go to the SUSPENDED State. In the SUSPENDED state all communication is stopped (both listening and receiving).
     * The connection is waiting until it receives instruction to reconnect.
     */
    public void goToSuspendedState() {
        // Update the state
        this.state = ConnectionState.SUSPENDED;

        if (this.handler == null) {
            // Handshake didn't even finish
            return;
        }

        // Notify Service implementation
        this.serviceExecutor.submit(() -> {
            try {
                this.handler.onSuspend();
            } catch (final Throwable e) {
                ManagedConnection.log.error("Error while calling onSuspend()", e);
            }
        });

        // Stop communication
        this.closePublishSocket();

        // Indicate that we have received no instruction to resume
        this.listenPort = 0;
        this.targetAddress = null;

    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.Connection#send(java.lang.Object)
     */
    @Override
    public void send(final Object message) {
        if (!this.getState().equals(ConnectionState.CONNECTED)) {
            ManagedConnection.log.warn("Unable to send when connection state is {}!", this.state);
            throw new IllegalStateException("Unable to send when connection state is " + this.state);
        }

        if (message == null) {
            return;
        }

        try {
            final byte[] data = this.serialize(message);
            if (!this.publishSocket.send(data)) {
                ManagedConnection.log.warn("Failed to send message through socket, goto {}",
                        ConnectionState.INTERRUPTED);
                this.goToInterruptedState();
            }
        } catch (final SerializationException e) {
            throw new IllegalArgumentException(e);
        } catch (final Exception e) {
            ManagedConnection.log
                    .error("Exception while sending message: {}, goto {}", e.getMessage(), ConnectionState.INTERRUPTED);
            ManagedConnection.log.trace(e.getMessage(), e);
            this.goToInterruptedState();
        }

    }

    /**
     * @param message
     * @return
     * @throws SerializationException
     */
    private byte[] serialize(final Object message) throws SerializationException {
        if (message instanceof byte[]) {
            return (byte[]) message;
        }

        if (!this.validateMessage(message)) {
            throw new SerializationException("Unable to serialize message of type '"
                    + message.getClass().getSimpleName() + "'. It is not defined in the interface");
        }

        return this.userMessageSerializer.serialize(message);
    }

    /**
     * @param message
     */
    private boolean validateMessage(final Object message) {
        for (final Class<?> c : this.info.sendTypes()) {
            if (c.isInstance(message)) {
                return true;
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.Connection#isConnected()
     */
    @Override
    public boolean isConnected() {
        return this.state == ConnectionState.CONNECTED;
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

    /**
     * ConnectionRunner
     *
     * @author coenvl
     * @version 0.1
     * @since Aug 22, 2017
     */
    public class ConnectionRunner implements Runnable {

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            long backOffMs = 100;
            while (ManagedConnection.this.state != ConnectionState.TERMINATED) {

                if (ManagedConnection.this.state == ConnectionState.STARTING) {
                    // State is STARTING, goal is to connect
                    final boolean success = ManagedConnection.this.tryConnectSending();
                    if (success) {
                        // Update state
                        ManagedConnection.this.state = ConnectionState.CONNECTED;

                        // reset backOff
                        backOffMs = 100;

                        // Initializing the connectionHandler involves invoking the constructor written by the user
                        ManagedConnection.this.initConnectionHandler();

                    } else {
                        backOffMs = Math.min(ManagedConnection.MAX_BACKOFF_MS, backOffMs * 2);
                        try {
                            Thread.sleep(backOffMs);
                        } catch (final InterruptedException e) {
                            // Don't care, we'll see you next iteration
                        }
                    }
                } else if (ManagedConnection.this.state == ConnectionState.CONNECTED) {
                    // State is CONNECTED, goal is to handle messages
                    ManagedConnection.this.tryReceiveMessage();

                } else if (ManagedConnection.this.state == ConnectionState.SUSPENDED) {
                    // State is SUSPENDED, there are two options: try to reconnect or wait for instructions to reconnect
                    if (ManagedConnection.this.listenPort == 0) {
                        // We are suspended and have received no instruction to resume, wait for instruction
                        synchronized (ManagedConnection.this.suspendLock) {
                            try {
                                ManagedConnection.this.suspendLock.wait();
                            } catch (final InterruptedException e) {
                                // Don't care, we'll see you next iteration
                            }
                        }
                    } else {
                        // We are suspended, but we want to go back to CONNECTED, try to connect
                        final boolean success = ManagedConnection.this.tryConnectSending();
                        if (success) {
                            // Update state
                            ManagedConnection.this.state = ConnectionState.CONNECTED;

                            // reset backOff
                            backOffMs = 100;

                            // Notify Service implementation
                            ManagedConnection.this.serviceExecutor.submit(() -> {
                                try {
                                    ManagedConnection.this.handler.resumeAfterSuspend();
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

                } else if (ManagedConnection.this.state == ConnectionState.INTERRUPTED) {
                    // State is INTERRUPTED, we have to try to reconnect
                    final boolean success = ManagedConnection.this.tryConnectSending();
                    if (success) {
                        // reset backOff
                        backOffMs = 100;

                        ManagedConnection.this.resumeAfterInterruptedState();
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
            ManagedConnection.log.debug("End of thread, cleaning up");
            ManagedConnection.this.close();
        }

    }

}
