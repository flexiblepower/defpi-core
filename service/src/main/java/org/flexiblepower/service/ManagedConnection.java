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
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;

import org.flexiblepower.exceptions.SerializationException;
import org.flexiblepower.proto.ConnectionProto.ConnectionState;
import org.flexiblepower.serializers.MessageSerializer;
import org.flexiblepower.service.exceptions.ConnectionModificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQException;

/**
 * ManagedConnection
 *
 * @version 0.1
 * @since May 12, 2017
 */
final class ManagedConnection implements Connection, Closeable {

    private static final int RECEIVE_TIMEOUT = 100;
    private static final int SEND_TIMEOUT = 200;

    protected static final Logger log = LoggerFactory.getLogger(ManagedConnection.class);
    private static int threadCount = 0;

    // private final String connectionId;
    private final Context zmqContext;
    private final HeartBeatMonitor heartBeat;
    private final HandShakeMonitor handShake;
    private final MessageSerializer<Object> userMessageSerializer;
    private final Thread connectionThread;

    private Socket subscribeSocket;
    private Socket publishSocket;
    private String targetAddress;

    protected volatile ConnectionState state;

    protected final InterfaceInfo info;
    protected final ExecutorService serviceExecutor;
    protected final Object suspendLock = new Object();

    protected int listenPort;
    protected ConnectionHandler serviceHandler;

    /**
     * @param connectionId
     * @param listenPort
     * @param targetAddress
     * @param info
     * @throws ConnectionModificationException
     */
    @SuppressWarnings("unchecked")
    ManagedConnection(final String connectionId,
            final int listenPort,
            final String targetAddress,
            final InterfaceInfo info) throws ConnectionModificationException {
        // this.connectionId = connectionId;
        this.listenPort = listenPort;
        this.targetAddress = targetAddress;
        this.info = info;

        this.serviceExecutor = ServiceMain.getServiceExecutor();
        this.heartBeat = new HeartBeatMonitor(this);
        this.handShake = new HandShakeMonitor(this, connectionId);
        this.state = ConnectionState.STARTING;

        // Add serializer to the connection for user-defined messages
        try {
            this.userMessageSerializer = this.info.serializer().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            ManagedConnection.log.error("Unable to instantiate connection serializer");
            throw new RuntimeException("Unable to instantiate connection serializer");
        }

        // Add sending and receiving types to serializer, don't add them twice
        final HashSet<Class<?>> classes = new HashSet<>();
        classes.addAll(Arrays.asList(this.info.sendTypes()));
        classes.addAll(Arrays.asList(this.info.receiveTypes()));
        for (final Class<?> messageType : classes) {
            this.userMessageSerializer.addMessageClass(messageType);
        }

        // Init ZMQ
        this.zmqContext = ZMQ.context(1);
        this.state = ConnectionState.STARTING;

        this.initSubscribeSocket();

        this.connectionThread = new Thread(new ConnectionRunner(),
                "dEF-Pi connThread-" + ManagedConnection.threadCount++);
        this.connectionThread.start();
    }

    private void initSubscribeSocket() {
        if (this.subscribeSocket != null) {
            ManagedConnection.log.debug("Re-creating subscribesocket");
            this.closeSubscribeSocket();
            try {
                // Let the socket close...
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

    protected boolean initPublishSocket() {
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

        if (this.handShake.sendHandshake(this.state)) {
            this.heartBeat.start();
            return true;
        } else {
            return false;
        }
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
            ManagedConnection.log.warn("send(Object message) method was called with null message, ignoring...");
            return;
        }

        if (!Arrays.asList(this.info.sendTypes()).contains(message.getClass())) {
            throw new IllegalArgumentException("The message type " + message.getClass().getName()
                    + " was not registered to be sent with this interface.");
        }

        final byte[] data;
        try {
            data = this.userMessageSerializer.serialize(message);
        } catch (final Exception e) {
            ManagedConnection.log.error("Error while serializing message, not sending message.", e);
            return;
        }

        try {
            if (!this.sendRaw(data)) {
                ManagedConnection.log.warn("Failed to send message through socket, goto {}",
                        ConnectionState.INTERRUPTED);
                this.goToInterruptedState();
            }
        } catch (final Exception e) {
            ManagedConnection.log
                    .error("Exception while sending message: {}, goto {}", e.getMessage(), ConnectionState.INTERRUPTED);
            ManagedConnection.log.trace(e.getMessage(), e);
        }
    }

    boolean sendRaw(final byte[] data) {
        return this.publishSocket.send(data);
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

    private synchronized void closePublishSocket() {
        if (this.publishSocket != null) {
            this.publishSocket.close();
            this.publishSocket = null;
        }
    }

    /**
     *
     */
    private synchronized void closeSubscribeSocket() {
        if (this.subscribeSocket != null) {
            this.subscribeSocket.close();
            this.subscribeSocket = null;
        }
    }

    @Override
    public synchronized void close() {
        this.state = ConnectionState.TERMINATED;

        this.closePublishSocket();
        this.closeSubscribeSocket();

        this.heartBeat.close();

        if (!this.zmqContext.isTerminated()) {
            // Close this in another thread, because it sometimes locks the VM
            (new Thread(() -> this.zmqContext.close())).start();
        }
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
                this.serviceHandler.onInterrupt();
            } catch (final Throwable e) {
                ManagedConnection.log.error("Error while calling onInterrupt()", e);
            }
        });
    }

    protected void resumeAfterInterruptedState() {
        // Update state
        this.state = ConnectionState.CONNECTED;

        // Notify Service implementation
        this.serviceExecutor.submit(() -> {
            try {
                this.serviceHandler.resumeAfterInterrupt();
            } catch (final Throwable e) {
                ManagedConnection.log.error("Error while calling resumeAfterInterrupt()", e);
            }
        });
    }

    protected void tryReceiveMessage() {
        byte[] buff = null;
        try {
            buff = this.subscribeSocket.recv();
        } catch (final ZMQException e) {
            ManagedConnection.log.warn("ZMQException while receiving message: {}", e.getMessage());
            ManagedConnection.log.trace(e.getMessage(), e);

            if (e.getErrorCode() == 156384765) {
                if (this.state == ConnectionState.CONNECTED) {
                    ManagedConnection.log.warn("Receive socket was disconnected.");
                    this.goToInterruptedState();
                }
                return;
            }
        } catch (final ClosedSelectorException | AssertionError e) {
            // The connection was suspended or terminated
            ManagedConnection.log.warn("Exception while receiving message: {}", e.getMessage());
            ManagedConnection.log.trace(e.getMessage(), e);
            return;
        }

        if (buff == null) {
            return;
        }

        // Check if it is a heart beat message.
        if (this.heartBeat.handleMessage(buff)) {
            return;
        }

        // Check if it is a handshake message.
        if (this.handShake.receiveHandShake(buff)) {
            return;
        }

        // It can only be a user-defined process message!
        try {
            final Object message = this.userMessageSerializer.deserialize(buff);
            final Class<?> messageType = message.getClass();
            final Method[] allMethods = this.serviceHandler.getClass().getMethods();
            for (final Method method : allMethods) {
                if ((method.getName().startsWith("handle")) && (method.getName().endsWith("Message"))
                        && (method.getParameterCount() == 1) && method.getParameterTypes()[0].equals(messageType)) {
                    this.serviceExecutor.submit(() -> {
                        try {
                            method.invoke(this.serviceHandler, message);
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
    void resumeAfterSuspendedState(final int newListenPort, final String newTargetAddress) {
        // Make sure we know that we want to go from SUSPENDED to RUNNING
        this.listenPort = newListenPort;
        this.targetAddress = newTargetAddress;

        // Make sure we are ready to listen
        this.initSubscribeSocket();

        // Wake up the main loop, so it tries to connect the sending
        synchronized (this.suspendLock) {
            this.suspendLock.notifyAll();
        }
    }

    void goToConnectedState() {
        this.state = ConnectionState.CONNECTED;

        if (this.serviceHandler == null) {
            // Initializing the connectionHandler involves invoking the constructor written by the user
            ManagedConnection.this.serviceHandler = ConnectionManager.buildHandlerForConnection(ManagedConnection.this,
                    ManagedConnection.this.info);
        }
    }

    void goToTerminatedState() {
        // Update the state
        this.state = ConnectionState.TERMINATED;

        if (this.serviceHandler != null) {
            // There is no handler if the handshake didn't finish, otherwise notify service implementation
            this.serviceExecutor.submit(() -> {
                try {
                    this.serviceHandler.terminated();
                } catch (final Throwable e) {
                    ManagedConnection.log.error("Error while calling terminated()", e);
                }
            });
        }

        // No need to close here, the main loop should take care of it...
    }

    /**
     * Go to the SUSPENDED State. In the SUSPENDED state all communication is stopped (both listening and receiving).
     * The connection is waiting until it receives instruction to reconnect.
     */
    void goToSuspendedState() {
        // Update the state
        this.state = ConnectionState.SUSPENDED;

        if (this.serviceHandler != null) {
            // There is no handler if the handshake didn't finish, otherwise notify service implementation
            this.serviceExecutor.submit(() -> {
                try {
                    this.serviceHandler.onSuspend();
                } catch (final Throwable e) {
                    ManagedConnection.log.error("Error while calling onSuspend()", e);
                }
            });
        }

        // Stop communication
        this.closePublishSocket();

        // Indicate that we have received no instruction to resume
        this.listenPort = 0;
        this.targetAddress = null;

    }

    void waitTillFinished() throws InterruptedException {
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

        private static final long INITIAL_BACKOFF_MS = 100;
        private static final long MAX_BACKOFF_MS = 60000;
        private long backOffMs = ConnectionRunner.INITIAL_BACKOFF_MS;

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            while (ManagedConnection.this.state != ConnectionState.TERMINATED) {
                ManagedConnection.this.tryReceiveMessage();

                if (ManagedConnection.this.state == ConnectionState.STARTING) {
                    // State is STARTING, goal is to connect
                    if (ManagedConnection.this.initPublishSocket()) {
                        this.resetBackOff();
                    } else {
                        this.increaseBackOffAndWait();
                    }
                } else

                if (ManagedConnection.this.state == ConnectionState.SUSPENDED) {
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
                        final boolean success = ManagedConnection.this.initPublishSocket();
                        if (success) {
                            // Update state
                            ManagedConnection.this.state = ConnectionState.CONNECTED;
                            this.resetBackOff();

                            // Notify Service implementation
                            ManagedConnection.this.serviceExecutor.submit(() -> {
                                try {
                                    ManagedConnection.this.serviceHandler.resumeAfterSuspend();
                                } catch (final Throwable e) {
                                    ManagedConnection.log.error("Error while calling resumeAfterSuspend()", e);
                                }
                            });
                        } else {
                            this.increaseBackOffAndWait();
                        }
                    }

                } else if (ManagedConnection.this.state == ConnectionState.INTERRUPTED) {
                    // State is INTERRUPTED, we have to try to reconnect
                    if (ManagedConnection.this.initPublishSocket()) {
                        this.resetBackOff();
                        ManagedConnection.this.resumeAfterInterruptedState();
                    } else {
                        this.increaseBackOffAndWait();
                    }
                }
            }
            // State is TERMINATED, cleanup
            ManagedConnection.log.debug("End of thread, cleaning up");
            ManagedConnection.this.close();
        }

        /**
         *
         */
        private void resetBackOff() {
            this.backOffMs = ConnectionRunner.INITIAL_BACKOFF_MS;
        }

        /**
         *
         */
        private void increaseBackOffAndWait() {
            this.backOffMs = Math.min(ConnectionRunner.MAX_BACKOFF_MS, this.backOffMs * 2);
            try {
                Thread.sleep(this.backOffMs);
            } catch (final InterruptedException e) {
                // Don't care, we'll see you next iteration
            }
        }

    }

}
