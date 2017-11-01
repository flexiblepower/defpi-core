/**
 * File java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.ClosedChannelException;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.flexiblepower.commons.TCPSocket;
import org.flexiblepower.exceptions.SerializationException;
import org.flexiblepower.proto.ConnectionProto.ConnectionState;
import org.flexiblepower.serializers.MessageSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ManagedConnection
 *
 * @version 0.1
 * @since May 12, 2017
 */
final class TCPConnection implements Connection, Closeable {

    protected static final Logger log = LoggerFactory.getLogger(TCPConnection.class);
    private static int threadCounter;

    private final ServiceExecutor serviceExecutor = ServiceExecutor.getInstance();
    private final SocketReader socketReader = new SocketReader();
    private final MessageSerializer<Object> userMessageSerializer;
    private final InterfaceInfo info;

    protected final ExecutorService connectionExecutor = Executors.newFixedThreadPool(4,
            r -> new Thread(r, "dEF-Pi connThread" + TCPConnection.threadCounter++));
    protected final MessageQueue messageQueue = new MessageQueue();
    protected final Object connectionLock = new Object();
    protected final String connectionId;

    private ConnectionHandler serviceHandler;

    protected int port;
    protected String targetAddress;
    protected TCPSocket socket;
    protected HeartBeatMonitor heartBeatMonitor;
    protected HandShakeMonitor handShakeMonitor;

    private volatile ConnectionState state;
    private final String remoteProcessId;
    private final String remoteServiceId;
    private final String remoteInterfaceId;

    /**
     * @param listenPort
     * @param targetAddress
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    TCPConnection(final String connectionId,
            final int port,
            final String targetAddress,
            final InterfaceInfo info,
            final String remoteProcessId,
            final String remoteServiceId,
            final String remoteInterfaceId) {
        this.state = ConnectionState.STARTING;
        this.connectionId = connectionId;
        this.port = port;
        this.targetAddress = targetAddress;
        this.info = info;
        this.remoteProcessId = remoteProcessId;
        this.remoteServiceId = remoteServiceId;
        this.remoteInterfaceId = remoteInterfaceId;

        // Add serializer to the connection for user-defined messages
        try {
            this.userMessageSerializer = info.serializer().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Unable to instantiate connection serializer");
        }

        // Add sending and receiving types to serializer
        Arrays.asList(info.sendTypes()).forEach((type) -> this.userMessageSerializer.addMessageClass(type));
        Arrays.asList(info.receiveTypes()).forEach((type) -> this.userMessageSerializer.addMessageClass(type));

        this.connectionExecutor.submit(this.messageQueue);
        this.connectionExecutor.submit(this.socketReader);
    }

    @Override
    public ConnectionState getState() {
        return this.state;
    }

    @Override
    public boolean isConnected() {
        return (this.state == ConnectionState.CONNECTED) && (this.handShakeMonitor != null)
                && this.handShakeMonitor.ready();
    }

    @Override
    public void send(final Object message) {
        if (message == null) {
            TCPConnection.log.warn("[{}] - Send(Object message) method was called with null message, ignoring...",
                    this.connectionId);
            return;
        }

        if (!this.isConnected()) {
            TCPConnection.log.warn("[{}] - Unable to send when connection state is {}!", this.connectionId, this.state);
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
            TCPConnection.log
                    .error("[{}] - Error while serializing message, not sending message.", this.connectionId, e);
            return;
        }

        try {
            this.socket.send(data);
        } catch (final IOException e) {
            TCPConnection.log.warn("[{}] - Failed to send message through socket, goto {}",
                    this.connectionId,
                    ConnectionState.INTERRUPTED);
            this.goToInterruptedState();
        } catch (final Exception e) {
            TCPConnection.log.error("[{}] - Exception while sending message: {}, goto {}",
                    this.connectionId,
                    e.getMessage(),
                    ConnectionState.INTERRUPTED);
            TCPConnection.log.trace(e.getMessage(), e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.Connection#otherProcessId()
     */
    @Override
    public String remoteProcessId() {
        return this.remoteProcessId;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.Connection#remoteServiceId()
     */
    @Override
    public String remoteServiceId() {
        return this.remoteServiceId;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.Connection#remoteInterfaceId()
     */
    @Override
    public String remoteInterfaceId() {
        return this.remoteInterfaceId;
    }

    protected void handleMessage(final byte[] msg) {
        // It can only be a user-defined process message!
        try {
            synchronized (this.connectionLock) {
                if (this.serviceHandler == null) {
                    try {
                        TCPConnection.log.warn("[{}] - Received message {} before connection is established. Hold...",
                                this.connectionId,
                                new String(msg).replaceAll("\0", "\\0"));
                        this.connectionLock.wait();
                        TCPConnection.log.trace("[{}] - continue...", this.connectionId);
                    } catch (final InterruptedException e) {
                        TCPConnection.log.trace(e.getMessage(), e);
                    }
                }
            }

            final Object message = this.userMessageSerializer.deserialize(msg);
            final Class<?> messageType = message.getClass();
            final Method[] allMethods = this.serviceHandler.getClass().getMethods();
            for (final Method method : allMethods) {
                if ((method.getName().startsWith("handle")) && (method.getName().endsWith("Message"))
                        && (method.getParameterCount() == 1) && method.getParameterTypes()[0].equals(messageType)) {
                    this.serviceExecutor.submit(() -> {
                        try {
                            method.invoke(this.serviceHandler, message);
                        } catch (IllegalAccessException | IllegalArgumentException e) {
                            TCPConnection.log.error("[{}] - Message handling method is not properly formatted",
                                    this.connectionId,
                                    e);
                        } catch (final InvocationTargetException e) {
                            TCPConnection.log.error("[{}] - Exception while invoking {} ({})",
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
            TCPConnection.log
                    .warn("[{}] - Received unknown message: {}. Ignoring...", this.connectionId, new String(msg));
        }
    }

    protected void releaseWaitLock() {
        synchronized (this.connectionLock) {
            this.connectionLock.notifyAll();
        }
    }

    void waitUntilConnected(final long millis) throws InterruptedException, IOException {
        if (this.isConnected()) {
            return;
        }

        synchronized (this.connectionLock) {
            if (millis < 1) {
                this.connectionLock.wait();
            } else {
                this.connectionLock.wait(millis);
            }
        }

        if (!this.isConnected()) {
            throw new ClosedChannelException();
        }
    }

    void goToConnectedState() {
        TCPConnection.log.info("[{}] - Going from {} to {}", this.connectionId, this.state, ConnectionState.CONNECTED);
        final ConnectionState previousState = this.state;
        this.state = ConnectionState.CONNECTED;

        switch (previousState) {
        case CONNECTED:
            TCPConnection.log.debug("[{}] - Ignoring goToConnected, already connected...", this.connectionId);
            return;
        case STARTING:
            this.serviceExecutor.submit(() -> {
                this.serviceHandler = ConnectionManager.buildHandlerForConnection(this, this.info);
                // this.releaseWaitLock();
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
            TCPConnection.log.error("[{}] - Unexpected previous state: {}", this.connectionId, this.state);
        }
        // Make sure it runs AFTER user constructor
        this.serviceExecutor.submit(() -> this.releaseWaitLock());
    }

    void goToSuspendedState() {
        if (!this.isConnected()) {
            TCPConnection.log.warn("[{}] - Not going to {} state while not connected",
                    this.connectionId,
                    ConnectionState.SUSPENDED);
            return;
        }

        // Update the state
        this.state = ConnectionState.SUSPENDED;
        this.heartBeatMonitor.stop();

        this.serviceExecutor.submit(() -> this.serviceHandler.onSuspend());
    }

    void goToResumedState(final int newListenPort, final String newTargetAddress) {
        if (this.state != ConnectionState.SUSPENDED) {
            TCPConnection.log.warn("[{}] - Unable to resume connection when not in {}",
                    this.connectionId,
                    ConnectionState.SUSPENDED);
            return;
        }

        this.port = newListenPort;
        this.targetAddress = newTargetAddress;

        // Have the socket reader reinstatiate the socket one
        this.socket.close();
        this.socket = null;
    }

    void goToInterruptedState() {
        if (!this.isConnected()) {
            TCPConnection.log.warn("[{}] - Not interrupting when not connected", this.connectionId);
            return;
        }

        this.state = ConnectionState.INTERRUPTED;
        if (this.serviceHandler != null) {
            this.serviceExecutor.submit(() -> this.serviceHandler.onInterrupt());
        }
    }

    void goToTerminatedState() {
        this.close();
    }

    @Override
    public synchronized void close() {
        // Update the state
        if (!this.state.equals(ConnectionState.TERMINATED)) {
            this.state = ConnectionState.TERMINATED;

            if (this.serviceHandler != null) {
                this.serviceExecutor.submit(() -> this.serviceHandler.terminated());
            }
        }

        this.socketReader.stop();

        if (this.heartBeatMonitor != null) {
            this.heartBeatMonitor.stop();
        }

        this.messageQueue.stop();

        if (this.socket != null) {
            this.socket.close();
            this.socket = null;
        }

        this.connectionExecutor.shutdownNow();
        try {
            this.connectionExecutor.awaitTermination(3, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            TCPConnection.log.error("Interrupted while awaiting termination");
        }

        this.releaseWaitLock();
    }

    private final class SocketReader implements Runnable {

        private volatile boolean keepRunning = true;

        /**
         *
         */
        protected SocketReader() {
            // Protected constructor for TCPConnection
        }

        @Override
        public void run() {
            while (this.keepRunning) {
                if (TCPConnection.this.socket != null) {
                    TCPConnection.log.info("Closing old socket");
                    TCPConnection.this.socket.close();
                }

                TCPConnection.log.info("Building TCPConnection");
                if (TCPConnection.this.targetAddress.isEmpty()) {
                    TCPConnection.this.socket = TCPSocket.asServer(TCPConnection.this.port);
                } else {
                    TCPConnection.this.socket = TCPSocket.asClient(TCPConnection.this.targetAddress,
                            TCPConnection.this.port);
                }

                try {
                    TCPConnection.this.socket.waitUntilConnected(0);
                } catch (final Exception e) {
                    if (this.keepRunning) {
                        TCPConnection.log.warn("Interrupted while waiting for connection to establish");
                        continue;
                    } else {
                        break;
                    }
                }

                // Create the monitors
                TCPConnection.this.handShakeMonitor = new HandShakeMonitor(TCPConnection.this.socket,
                        TCPConnection.this.connectionId);
                TCPConnection.this.heartBeatMonitor = new HeartBeatMonitor(TCPConnection.this.socket,
                        TCPConnection.this.connectionId);

                // Now we have a functioning socket, make sure that as soon as there is a handshake, go connected
                TCPConnection.this.connectionExecutor.submit(() -> {
                    try {
                        TCPConnection.this.handShakeMonitor.sendHandshake(TCPConnection.this.getState());
                        TCPConnection.this.handShakeMonitor.waitUntilFinished(0);
                        TCPConnection.this.heartBeatMonitor.start();
                        TCPConnection.this.goToConnectedState();
                    } catch (final InterruptedException e) {
                        if (this.keepRunning) {
                            TCPConnection.log.warn("Interrupted while waiting for TCP socket to initialize");
                        }
                    }
                });

                while (this.keepRunning && TCPConnection.this.socket.isConnected()) {
                    try {
                        final byte[] data = TCPConnection.this.socket.read();

                        if ((data == null) || (data.length == 0)) {
                            continue;
                        }

                        if (!TCPConnection.this.heartBeatMonitor.handleMessage(data)
                                && !TCPConnection.this.handShakeMonitor.handleHandShake(data)) {
                            TCPConnection.this.messageQueue.addMessage(data);
                        }
                    } catch (final IOException | InterruptedException e) {
                        // See if this was on purpose
                        if (TCPConnection.this.isConnected() && this.keepRunning) {
                            TCPConnection.log.warn("IOException while reading from socket: {}", e.getMessage());
                            TCPConnection.log.trace(e.getMessage(), e);
                            TCPConnection.this.goToInterruptedState();
                        }
                        break;
                    }
                }

                // Don't hog the thread...
                try {
                    Thread.sleep(10);
                } catch (final InterruptedException e) {
                    // It's okay
                }

                // Retry setting up the connection
            }
        }

        public void stop() {
            this.keepRunning = false;
        }

    }

    private final class MessageQueue implements Runnable {

        private final BlockingQueue<byte[]> internalQueue = new LinkedBlockingQueue<>();
        private volatile boolean keepRunning = true;

        /**
         *
         */
        protected MessageQueue() {
            // Protected constructor for TCPConnection
        }

        public void addMessage(final byte[] msg) {
            try {
                this.internalQueue.put(msg);
            } catch (final InterruptedException e) {
                TCPConnection.log.warn("Interrupted while adding message to queue");
            }
        }

        @Override
        public void run() {
            while (this.keepRunning) {
                try {
                    final byte[] message = this.internalQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (message != null) {
                        TCPConnection.this.handleMessage(message);
                    }
                } catch (final InterruptedException e) {
                    TCPConnection.log.trace("Message handler interrupted, stopping thread");
                    break;
                }
            }
        }

        public void stop() {
            this.keepRunning = false;
        }

    }

}
