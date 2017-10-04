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
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
    // private static int threadCount = 0;
    private static final int MAX_THREADS = 10;

    private final String connectionId;
    private final Context zmqContext;
    private final MessageSerializer<Object> userMessageSerializer;

    private Socket subscribeSocket;
    private Socket publishSocket;
    private String targetAddress;

    private final InterfaceInfo info;
    private final ServiceExecutor serviceExecutor = ServiceExecutor.getInstance();
    private final ExecutorService connectionExecutor;
    // private final Object suspendLock = new Object();
    private final HeartBeatMonitor heartBeat;

    private int listenPort;
    private ConnectionHandler serviceHandler;

    protected volatile ConnectionState state;
    protected final HandShakeMonitor handShake;
    private final Object messageLock = new Object();

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
        this.connectionId = connectionId;
        this.listenPort = listenPort;
        this.targetAddress = targetAddress;
        this.info = info;

        this.connectionExecutor = Executors.newFixedThreadPool(ManagedConnection.MAX_THREADS,
                r -> new Thread(r, "dEF-Pi connThread"));
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

        this.connectionExecutor.submit(new ConnectionRunner());
        // new Thread(new ConnectionRunner(), "dEF-Pi connThread").start();
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

        this.connectionExecutor.submit(() -> {
            while (this.state != ConnectionState.TERMINATED) {
                this.tryReceiveMessage();
            }
        });

        // new Thread(() -> {
        // while (this.state != ConnectionState.TERMINATED) {
        // this.tryReceiveMessage();
        // }
        // }, "dEF-Pi recvThread").start();
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

        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.Connection#send(java.lang.Object)
     */
    @Override
    public void send(final Object message) {
        if (message == null) {
            ManagedConnection.log.warn("send(Object message) method was called with null message, ignoring...");
            return;
        }

        if (!this.isConnected()) {
            ManagedConnection.log.warn("Unable to send when connection state is {}!", this.state);
            throw new IllegalStateException("Unable to send when connection state is " + this.state);
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
        return (this.publishSocket == null) || this.publishSocket.send(data);
    }

    protected void tryReceiveMessage() {
        if (this.subscribeSocket == null) {
            // This is possible if the socket was closed in another thread
            return;
        }

        try {
            final byte[] buff = this.subscribeSocket.recv();
            if ((buff == null) || (buff.length == 0)) {
                return;
            } else {
                this.connectionExecutor.submit(() -> this.handleMessage(buff));
            }
        } catch (final ZMQException e) {

            // if (e.getErrorCode() == 156384765) {
            if (this.state == ConnectionState.CONNECTED) {
                ManagedConnection.log.warn("ZMQException while receiving message: {}", e.getMessage());
                ManagedConnection.log.trace(e.getMessage(), e);
                // ManagedConnection.log.warn("Receive socket was disconnected.");
                this.goToInterruptedState();
            }
            return;
            // }
        } catch (final ClosedSelectorException | AssertionError e) {
            // The connection was suspended or terminated
            ManagedConnection.log.warn("Exception while receiving message: {}", e.getMessage());
            ManagedConnection.log.trace(e.getMessage(), e);
            return;
        }

    }

    private void handleMessage(final byte[] buff) {

        if (buff == null) {
            return;
        }

        // Check if it is a heart beat message.
        if (this.heartBeat.handleMessage(buff)) {
            return;
        }

        // Check if it is a handshake message.
        if (this.handShake.handleHandShake(buff)) {
            return;
        }

        // It can only be a user-defined process message!
        try {
            synchronized (this.messageLock) {
                if (this.serviceHandler == null) {
                    try {
                        ManagedConnection.log.warn("Received message {} before connection is established. Hold...",
                                new String(buff));
                        this.messageLock.wait();
                        ManagedConnection.log.trace("continue...");
                    } catch (final InterruptedException e) {
                        ManagedConnection.log.trace(e.getMessage(), e);
                    }
                }
            }

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

    /**
     * Instructs the managedConnection to go to the CONNECTED state. This involves triggering the relevant user method
     * in a separate thread and starting the heartbeat
     */
    void goToConnectedState() {
        // ManagedConnection.log
        // .info("Connection {} going from {} to {}", this.connectionId, this.state, ConnectionState.CONNECTED);
        final ConnectionState previousState = this.state;
        this.state = ConnectionState.CONNECTED;

        switch (previousState) {
        case CONNECTED:
            ManagedConnection.log.debug("Ignoring goToConnected, already connected...");
            return;
        case STARTING:
            // This can only be true the first time, create the service handler!
            this.serviceExecutor.submit(() -> {
                synchronized (this.messageLock) {
                    ManagedConnection.this.serviceHandler = ConnectionManager
                            .buildHandlerForConnection(ManagedConnection.this, ManagedConnection.this.info);
                    this.messageLock.notifyAll();
                }
            });
            break;
        case INTERRUPTED:
            this.serviceExecutor.submit(() -> this.serviceHandler.resumeAfterInterrupt());
            break;
        case SUSPENDED:
            this.serviceExecutor.submit(() -> this.serviceHandler.resumeAfterSuspend());
            break;
        case TERMINATED:
        default:
            ManagedConnection.log.error("Unexpected previous state: {}", this.state);
        }

        this.heartBeat.start();
    }

    /**
     * Go to the INTERRUPTED state. This method is called when sending fails or when no heartbeats are received.
     */
    void goToInterruptedState() {
        if (this.state == ConnectionState.INTERRUPTED) {
            ManagedConnection.log.warn("Already in {} state, not interrupting", ConnectionState.INTERRUPTED);
            return;
        }

        if (this.serviceHandler == null) {
            ManagedConnection.log.warn("ServiceHandler not yet instantiated, not interrupting...");
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

        // Start a thread to fix the connection
        this.connectionExecutor.submit(new ConnectionRunner());
        // new Thread(new ConnectionRunner(), "dEF-Pi connThread").start();
    }

    /**
     * Go to the SUSPENDED State. In the SUSPENDED state all communication is stopped (both listening and receiving).
     * The connection is waiting until it receives instruction to reconnect.
     */
    void goToSuspendedState() {
        if (!this.isConnected()) {
            ManagedConnection.log.warn("Not going to {} state while not connected", ConnectionState.SUSPENDED);
            return;
        }

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

    /**
     * Try to go back from SUSPENDED to CONNECTED. This message prepares the main loop to try to connect again.
     *
     * @param listenPort
     * @param targetAddress
     */
    void goToResumedState(final int newListenPort, final String newTargetAddress) {
        if (this.state != ConnectionState.SUSPENDED) {
            ManagedConnection.log.warn("Unable to resume connection when not in {}", ConnectionState.SUSPENDED);
            return;
        }

        // Make sure we know that we want to go from SUSPENDED to RUNNING
        this.listenPort = newListenPort;
        this.targetAddress = newTargetAddress;

        // Initialize a (new) publish socket with the new target
        this.initSubscribeSocket();

        // Make sure we fix the connection
        this.connectionExecutor.submit(new ConnectionRunner());
        // new Thread(new ConnectionRunner(), "conn-fix").start();
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

        this.close();
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
    public void close() {
        this.state = ConnectionState.TERMINATED;

        this.closePublishSocket();
        this.closeSubscribeSocket();

        if (this.heartBeat != null) {
            this.heartBeat.close();
        }

        if (!this.zmqContext.isTerminated() && !this.connectionExecutor.isShutdown()) {
            // Close this in another thread, because it sometimes locks the VM
            this.connectionExecutor.execute(() -> this.zmqContext.close());
            // (new Thread(() -> this.zmqContext.close())).start();
        }

        if (!this.connectionExecutor.isShutdown()) {
            this.connectionExecutor.shutdown();
            try {
                if (!this.connectionExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                    // Force shutdown
                    this.connectionExecutor.shutdownNow();
                }
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
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
                while (!ManagedConnection.this.initPublishSocket()) {
                    this.increaseBackOffAndWait();
                }

                if (ManagedConnection.this.handShake.sendHandshake(ManagedConnection.this.state)) {
                    // The handshake is sent successfully, now wait until the other side confirms. If it does not, retry
                    /*
                     * try {
                     * Thread.sleep(100);
                     * } catch (final InterruptedException e) {
                     * // Do nothing
                     * }
                     */
                    ManagedConnection.log.debug("Connection handshake sent, end-of-thread");
                    return;
                } else {
                    // If the handshake is not sent, we want to re-init the socket.
                    this.increaseBackOffAndWait();
                }
            }

            // State is TERMINATED, cleanup
            ManagedConnection.log.debug("End of connection thread");
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
