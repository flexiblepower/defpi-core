/**
 * File ManagedConnection.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
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

/**
 * ManagedConnection
 *
 * @version 0.1
 * @since May 12, 2017
 */
final class ManagedConnection implements Connection, Closeable {

    // private static final int RECEIVE_TIMEOUT = 100;
    // private static final int SEND_TIMEOUT = 200;

    protected static final Logger log = LoggerFactory.getLogger(ManagedConnection.class);
    // private static int threadCount = 0;
    private static final int MAX_THREADS = 10;

    protected final String connectionId;
    private final MessageSerializer<Object> userMessageSerializer;

    private ServerSocket serverSocket;
    private Socket subscribeSocket;
    private Socket publishSocket;
    private OutputStream os;
    private InputStream is;
    private String targetAddress;

    private final InterfaceInfo info;
    private final ServiceExecutor serviceExecutor = ServiceExecutor.getInstance();
    private final ExecutorService connectionExecutor;
    // private final Object suspendLock = new Object();
    private final HeartBeatMonitor heartBeat;
    private final ConnectionRunner connectionRunner = new ConnectionRunner();

    private int listenPort;
    private ConnectionHandler serviceHandler;

    protected volatile ConnectionState state;
    protected final HandShakeMonitor handShake;
    private final Object handlerLock = new Object();
    protected final Object handshakeLock = new Object();
    private static int threadCounter;

    /**
     * @param connectionId
     * @param listenPort
     * @param targetAddress
     * @param info
     * @throws IOException
     * @throws ConnectionModificationException
     */
    @SuppressWarnings("unchecked")
    ManagedConnection(final String connectionId,
            final int listenPort,
            final String targetAddress,
            final InterfaceInfo info) throws IOException {
        this.connectionId = connectionId;
        this.listenPort = listenPort;
        this.targetAddress = targetAddress;
        this.info = info;

        this.connectionExecutor = Executors.newFixedThreadPool(ManagedConnection.MAX_THREADS,
                r -> new Thread(r, "dEF-Pi connThread" + ManagedConnection.threadCounter++));
        this.heartBeat = new HeartBeatMonitor(this, connectionId);
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
        this.state = ConnectionState.STARTING;

        this.initSubscribeSocket();

        this.connectionExecutor.submit(() -> this.connectionRunner.fixConnection());
        // new Thread(new ConnectionRunner(), "dEF-Pi connThread").start();
    }

    private void initSubscribeSocket() throws IOException {
        if (this.subscribeSocket != null) {
            ManagedConnection.log.debug("[{}] - Re-creating subscribesocket", this.connectionId);
            this.closeSubscribeSocket();
        }

        ManagedConnection.log
                .debug("[{}] - Creating subscribeSocket listening on port {}", this.connectionId, this.listenPort);
        this.serverSocket = new ServerSocket(this.listenPort);

        this.connectionExecutor.submit(() -> {
            try {
                this.subscribeSocket = this.serverSocket.accept();
                ManagedConnection.log.info("Received client connection from {}",
                        this.subscribeSocket.getRemoteSocketAddress());
                this.is = this.subscribeSocket.getInputStream();
            } catch (final Exception e) {
                ManagedConnection.log.error("Error while creating subscribe socket: {}", e.getMessage());
                ManagedConnection.log.trace(e.getMessage(), e);
                return;
            }

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
        final String[] targetParts = this.targetAddress.split("[:/]+");
        // Try to connect
        try {
            ManagedConnection.log
                    .debug("[{}] - Creating publishSocket sending to {}", this.connectionId, this.targetAddress);

            this.publishSocket = new Socket(targetParts[1], Integer.parseInt(targetParts[2]));
            if (!this.publishSocket.isConnected() || this.publishSocket.isClosed()) {
                ManagedConnection.log.debug("[{}] - Failed to connect to {}, remote side not ready?",
                        this.connectionId);
                return false;
            }
            this.os = this.publishSocket.getOutputStream();
        } catch (final Exception e) {
            // Could not resolve hostname, other container is not yet ready
            ManagedConnection.log
                    .debug("[{}] - Exception while connecting to remote: {}", this.connectionId, e.getMessage());
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
            ManagedConnection.log.warn("[{}] - Send(Object message) method was called with null message, ignoring...",
                    this.connectionId);
            return;
        }

        if (!this.isConnected()) {
            ManagedConnection.log
                    .warn("[{}] - Unable to send when connection state is {}!", this.connectionId, this.state);
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
            ManagedConnection.log
                    .error("[{}] - Error while serializing message, not sending message.", this.connectionId, e);
            return;
        }

        try {
            if (!this.sendRaw(data)) {
                ManagedConnection.log.warn("[{}] - Failed to send message through socket, goto {}",
                        this.connectionId,
                        ConnectionState.INTERRUPTED);
                this.goToInterruptedState();
            }
        } catch (final Exception e) {
            ManagedConnection.log.error("[{}] - Exception while sending message: {}, goto {}",
                    this.connectionId,
                    e.getMessage(),
                    ConnectionState.INTERRUPTED);
            ManagedConnection.log.trace(e.getMessage(), e);
        }
    }

    boolean sendRaw(final byte[] data) {
        if (this.publishSocket == null) {
            return false;
        }
        try {
            // ManagedConnection.log.trace("Writing {} bytes to stream", data.length);
            this.os.write(data.length / 256);
            this.os.write(data.length % 256);
            this.os.write(data);
            this.os.write(0xFF);
            this.os.flush();
            return true;
        } catch (final Exception e) {
            ManagedConnection.log.warn("Error writing to stream: {}", e.getMessage());
            ManagedConnection.log.trace(e.getMessage(), e);
            return false;
        }
    }

    protected void tryReceiveMessage() {
        // try {
        // // Make sure we don't hog the subscribe lock
        // Thread.sleep(0, 100);
        // } catch (final InterruptedException e) {
        // // Interrupted?
        // }
        // synchronized (this.subscribeLock) {
        if (this.subscribeSocket == null) {
            // This is possible if the socket was closed in another thread
            return;
        }

        try {
            // final byte[] data = new byte[0];
            final int len = (this.is.read() * 256) + this.is.read();
            if (len < 0) {
                if (this.isConnected()) {
                    ManagedConnection.log.debug("End of stream reached");
                    this.goToInterruptedState();
                }
                return;
            }

            final byte[] data = new byte[len];
            // final byte[] buf = new byte[1024];
            final int read = this.is.read(data);
            if (read != len) {
                ManagedConnection.log.warn("Expected {} bytes, only received {}", len, read);
            }
            int eof = this.is.read();
            if (eof != 0xFF) {
                ManagedConnection.log.warn("Expected EOF, skipping stream");
                while (eof != 0xFF) {
                    eof = this.is.read();
                }
            }

            // ManagedConnection.log.debug("Received: {}", new String(data));
            this.connectionExecutor.submit(() -> this.handleMessage(data));

        } catch (final Exception e) {
            // The connection is probably suspended or terminated
            if (!e.getClass().equals(SocketException.class) || !e.getMessage().equals("Socket closed")) {
                ManagedConnection.log
                        .warn("[{}] - Exception while receiving message: {}", this.connectionId, e.getMessage());
                ManagedConnection.log.trace(e.getMessage(), e);
            }
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
            synchronized (this.handlerLock) {
                if (this.serviceHandler == null) {
                    try {
                        ManagedConnection.log.warn(
                                "[{}] - Received message {} before connection is established. Hold...",
                                this.connectionId,
                                new String(buff).replaceAll("\0", "\\0"));
                        this.handlerLock.wait();
                        ManagedConnection.log.trace("[{}] - continue...", this.connectionId);
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
                            ManagedConnection.log.error("[{}] - Message handling method is not properly formatted",
                                    this.connectionId,
                                    e);
                        } catch (final InvocationTargetException e) {
                            ManagedConnection.log.error("[{}] - Exception while invoking {} ({})",
                                    this.connectionId,
                                    method.getName(),
                                    messageType.getSimpleName(),
                                    e.getTargetException());
                        }
                    });
                }
            }
        } catch (final SerializationException e) {
            // Not a user-defined message, so ignore with grace!
            ManagedConnection.log
                    .warn("[{}] - Received unknown message: {}. Ignoring...", this.connectionId, new String(buff));
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
        ManagedConnection.log
                .info("Connection {} going from {} to {}", this.connectionId, this.state, ConnectionState.CONNECTED);
        final ConnectionState previousState = this.state;
        this.state = ConnectionState.CONNECTED;

        switch (previousState) {
        case CONNECTED:
            ManagedConnection.log.debug("[{}] - Ignoring goToConnected, already connected...", this.connectionId);
            return;
        case STARTING:
            // Service handler is created by connectionRunner
            break;
        case INTERRUPTED:
            this.serviceExecutor.submit(() -> this.serviceHandler.resumeAfterInterrupt());
            break;
        case SUSPENDED:
            this.serviceExecutor.submit(() -> this.serviceHandler.resumeAfterSuspend());
            break;
        case TERMINATED:
        default:
            ManagedConnection.log.error("[{}] - Unexpected previous state: {}", this.connectionId, this.state);
        }

        synchronized (this.handshakeLock) {
            this.handshakeLock.notifyAll();
        }
        this.heartBeat.start();
    }

    /**
     * Go to the INTERRUPTED state. This method is called when sending fails or when no heartbeats are received.
     */
    void goToInterruptedState() {
        if (this.state == ConnectionState.INTERRUPTED) {
            ManagedConnection.log.warn("[{}] - Already in {} state, not interrupting",
                    this.connectionId,
                    ConnectionState.INTERRUPTED);
            return;
        }

        if (this.serviceHandler == null) {
            ManagedConnection.log.warn("[{}] - ServiceHandler not yet instantiated, not interrupting...",
                    this.connectionId);
            return;
        }

        // Update state
        this.state = ConnectionState.INTERRUPTED;

        // Notify Service implementation
        this.serviceExecutor.submit(() -> {
            try {
                this.serviceHandler.onInterrupt();
            } catch (final Throwable e) {
                ManagedConnection.log.error("[{}] - Error while calling onInterrupt()", this.connectionId, e);
            }
        });

        // Start a thread to fix the connection
        try {
            this.initSubscribeSocket();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        this.connectionExecutor.submit(() -> this.connectionRunner.fixConnection());
        // new Thread(new ConnectionRunner(), "dEF-Pi connThread").start();
    }

    /**
     * Go to the SUSPENDED State. In the SUSPENDED state all communication is stopped (both listening and receiving).
     * The connection is waiting until it receives instruction to reconnect.
     */
    void goToSuspendedState() {
        if (!this.isConnected()) {
            ManagedConnection.log.warn("[{}] - Not going to {} state while not connected",
                    this.connectionId,
                    ConnectionState.SUSPENDED);
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
                    ManagedConnection.log.error("[{}] - Error while calling onSuspend()", this.connectionId, e);
                }
            });
        }

        // Stop communication
        // this.closeSubscribeSocket();
        // this.closePublishSocket();

        // Indicate that we have received no instruction to resume
        this.listenPort = 0;
        this.targetAddress = null;

    }

    /**
     * Try to go back from SUSPENDED to CONNECTED. This message prepares the main loop to try to connect again.
     *
     * @param listenPort
     * @param targetAddress
     * @throws IOException
     */
    void goToResumedState(final int newListenPort, final String newTargetAddress) throws IOException {
        if (this.state != ConnectionState.SUSPENDED) {
            ManagedConnection.log.warn("[{}] - Unable to resume connection when not in {}",
                    this.connectionId,
                    ConnectionState.SUSPENDED);
            return;
        }

        // Make sure we know that we want to go from SUSPENDED to RUNNING
        this.listenPort = newListenPort;
        this.targetAddress = newTargetAddress;

        // Initialize a (new) subscribe socket with the new port
        this.initSubscribeSocket();

        // Create a new publish socket, and make sure we are connected
        this.connectionExecutor.submit(() -> this.connectionRunner.fixConnection());
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
                    ManagedConnection.log.error("[{}] - Error while calling terminated()", this.connectionId, e);
                }
            });
        }

        this.close();
    }

    private void closePublishSocket() {
        if (this.publishSocket != null) {
            try {
                this.os.close();
                this.publishSocket.close();
            } catch (final IOException e) {
                ManagedConnection.log.warn("Exception while closing socket: {}", e.getMessage());
            }
            this.publishSocket = null;
        }
    }

    /**
     *
     */
    private void closeSubscribeSocket() {
        if (this.subscribeSocket != null) {
            try {
                this.is.close();
                this.subscribeSocket.close();
            } catch (final IOException e) {
                ManagedConnection.log.warn("Exception while closing socket: {}", e.getMessage());
            }
            this.subscribeSocket = null;
        }

        if (this.serverSocket != null) {
            try {
                this.serverSocket.close();
            } catch (final IOException e) {
                ManagedConnection.log.warn("Exception while closing server socket: {}", e.getMessage());
            }
            this.serverSocket = null;
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

        if (!this.connectionExecutor.isShutdown()) {
            ManagedConnection.log.debug("[{}] - Shutting down connection threads", this.connectionId);
            this.connectionExecutor.shutdown();
            try {
                if (!this.connectionExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                    // Force shutdown
                    ManagedConnection.log.debug("[{}] - Force shutting down connection threads", this.connectionId);
                    this.connectionExecutor.shutdownNow();
                }
            } catch (final InterruptedException e) {
                ManagedConnection.log.warn("[{}] - Interupted while shutting down connection threads",
                        this.connectionId);
            }
        }

        // if (!this.zmqContext.isTerminated()) {
        // // Close this in another thread, because it sometimes locks the VM
        // new Thread(() -> this.zmqContext.close()).start();
        // }
    }

    /**
     * ConnectionRunner
     *
     * @author coenvl
     * @version 0.1
     * @since Aug 22, 2017
     */
    public class ConnectionRunner {

        private static final long INITIAL_BACKOFF_MS = 100;
        private static final long MAX_BACKOFF_MS = 60000;
        private long backOffMs = ConnectionRunner.INITIAL_BACKOFF_MS;

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Runnable#run()
         */
        public synchronized void fixConnection() {
            this.backOffMs = ConnectionRunner.INITIAL_BACKOFF_MS;
            while ((ManagedConnection.this.state != ConnectionState.TERMINATED)
                    && (ManagedConnection.this.state != ConnectionState.CONNECTED)) {

                while (!ManagedConnection.this.initPublishSocket()) {
                    this.increaseBackOffAndWait();
                }

                if (!ManagedConnection.this.handShake.sendHandshake(ManagedConnection.this.state)) {
                    // If the handshake is not sent, we want to re-init the socket.
                    this.increaseBackOffAndWait();
                }

                // The handshake is sent successfully, now wait until the other side confirms. If it does not, retry
                try {
                    synchronized (ManagedConnection.this.handshakeLock) {
                        ManagedConnection.this.handshakeLock.wait(500);
                    }
                } catch (final InterruptedException e) {
                    // Do nothing
                }

                if (ManagedConnection.this.state.equals(ConnectionState.CONNECTED)) {
                    ManagedConnection.log.info("[{}] - Connection established", ManagedConnection.this.connectionId);

                    // If this is the first time, build the serviceHandler
                    if (ManagedConnection.this.serviceHandler == null) {
                        ManagedConnection.this.serviceExecutor.submit(() -> {
                            synchronized (ManagedConnection.this.handlerLock) {
                                ManagedConnection.this.serviceHandler = ConnectionManager
                                        .buildHandlerForConnection(ManagedConnection.this, ManagedConnection.this.info);
                                ManagedConnection.this.handlerLock.notifyAll();
                            }
                        });
                    }

                    return;
                } else {
                    ManagedConnection.log.warn("[{}] - Expected connection to be restored...",
                            ManagedConnection.this.connectionId);
                    continue;
                }
            }

            // State is TERMINATED, cleanup
            ManagedConnection.log.debug("[{}] - End of connection thread", ManagedConnection.this.connectionId);
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
