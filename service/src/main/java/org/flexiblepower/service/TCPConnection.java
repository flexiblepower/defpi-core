/*-
 * #%L
 * dEF-Pi service managing library
 * %%
 * Copyright (C) 2017 - 2018 Flexible Power Alliance Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
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

import javax.activation.UnsupportedDataTypeException;

import org.flexiblepower.commons.TCPSocket;
import org.flexiblepower.exceptions.SerializationException;
import org.flexiblepower.proto.ConnectionProto.ConnectionState;
import org.flexiblepower.serializers.MessageSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The TCPConnection implements the Connection interface with a TCPSocket as the underlying mechanism to send and
 * receive the raw byte arrays. It utilizes a {@link HeartBeatMonitor} and a {@link HandShakeMonitor} to ensure the
 * health status of the connection.
 *
 * @version 0.1
 * @since May 12, 2017
 */
final class TCPConnection implements Connection, Closeable {

    /**
     * Log for any events in this TCPConnection or subclasses.
     */
    protected static final Logger log = LoggerFactory.getLogger(TCPConnection.class);
    /**
     * Keeps track of how many threads the TCPConnection class has spawned. Very useful for logging
     */
    private static int threadCounter;

    private final ServiceExecutor serviceExecutor = ServiceExecutor.getInstance();
    private final SocketReader socketReader = new SocketReader();
    private final MessageSerializer<Object> userMessageSerializer;
    private final InterfaceInfo info;

    /**
     * The connection executor is the pool of threads that will run the different services to keep a connection alive,
     * and read its messages
     */
    protected final ExecutorService connectionExecutor = Executors
            .newCachedThreadPool(r -> new Thread(r, "dEF-Pi connThread" + TCPConnection.threadCounter++));

    /**
     * A runnable object that will make sure the messages in the queue are given to the responsible ConnectionHandler
     */
    protected final MessageQueue messageQueue = new MessageQueue();

    /**
     * A wait/lock object to make sure various functions in the Connection will wait until the connection is
     * established.
     */
    protected final Object connectionLock = new Object();

    /**
     * A string uniquely identifying this specific connection.
     */
    protected final String connectionId;

    /**
     * The handler object from user code that handles all incoming messages
     */
    private ConnectionHandler serviceHandler;

    /**
     * The port to listen or target remotely (depending on if {@link #targetAddress} is set
     */
    protected int port;

    /**
     * The remote host address to target the TCPSocket to
     */
    protected String targetAddress;

    /**
     * The socket that provides the underlying message carrying mechanism. This may be closed and reinitialized as the
     * Connection is interrupted, suspended, or any transient intermediate state
     */
    protected volatile TCPSocket socket;

    /**
     * The heartbeat monitor is an external object that periodically checks if the connection is still healthy.
     */
    protected HeartBeatMonitor heartBeatMonitor;

    /**
     * The handshake monitor is an external object to make sure the remote side is also initialized and has the correct
     * protocol, and thus remote ConnectionHAndler
     */
    protected HandShakeMonitor handShakeMonitor;

    private volatile ConnectionState state;
    private final String remoteProcessId;
    private final String remoteServiceId;
    private final String remoteInterfaceId;

    /**
     * @param connectionId The unique id if this connection, as specified by the orchestrator
     * @param port the TCP port to attach to when this endpoint will act as server, or the remote address to
     *            connect to.
     * @param targetAddress The host name of the remote endpoint to connect to when this endpoint should act as a
     *            client, or an empty string when it should act as a server.
     * @param info the InterfaceInfo the appropriate ConnectionHandler is annotated with.
     * @param remoteProcessId The process ID of the remote endpoint as specified by the orchestrator
     * @param remoteServiceId The service ID of the remote process as specified by the orchestrator
     * @param remoteInterfaceId The interface ID of the remote service
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
            this.userMessageSerializer = info.serializer().getConstructor().newInstance();
        } catch (final Exception e) {
            throw new RuntimeException("Unable to instantiate connection serializer");
        }

        // Add sending and receiving types to serializer
        Arrays.asList(info.sendTypes()).forEach(this.userMessageSerializer::addMessageClass);
        Arrays.asList(info.receiveTypes()).forEach(this.userMessageSerializer::addMessageClass);

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

    /**
     * {@inheritDoc}
     *
     * @throws ClosedChannelException when the state is not connected.
     * @throws UnsupportedDataTypeException when the type of object is not registered with the serializer or if the
     *             serialization fails
     * @throws IOException when a low level network exception occurs
     */
    @Override
    public void send(final Object message) throws IOException {
        if (message == null) {
            TCPConnection.log.warn("[{}] - Send(Object message) method was called with null message, ignoring...",
                    this.connectionId);
            return;
        }

        if (!this.isConnected()) {
            TCPConnection.log.warn("[{}] - Unable to send when connection state is {}!", this.connectionId, this.state);
            throw new ClosedChannelException();
        }

        if (!Arrays.asList(this.info.sendTypes()).contains(message.getClass())) {
            throw new UnsupportedDataTypeException("The message type " + message.getClass().getName()
                    + " was not registered to be sent with this interface.");
        }

        final byte[] data;
        try {
            synchronized (this.userMessageSerializer) {
                data = this.userMessageSerializer.serialize(message);
            }
        } catch (final SerializationException e) {
            TCPConnection.log
                    .error("[{}] - Error while serializing message, not sending message.", this.connectionId, e);
            throw new UnsupportedDataTypeException("Error serializing message: " + e.getMessage());
        }

        try {
            this.socket.send(data);
        } catch (final IOException e) {
            TCPConnection.log.warn("[{}] - Failed to send message through socket, goto {}",
                    this.connectionId,
                    ConnectionState.INTERRUPTED);
            this.goToInterruptedState();
            throw e;
        }
    }

    @Override
    public String remoteProcessId() {
        return this.remoteProcessId;
    }

    @Override
    public String remoteServiceId() {
        return this.remoteServiceId;
    }

    @Override
    public String remoteInterfaceId() {
        return this.remoteInterfaceId;
    }

    /**
     * When a new byte array is read by the underlying TCPSocket, this function is called to make sure the appropriate
     * handler function is called by the user object implementing the ConnectionHandler.
     * <p>
     * If this function is called before the connection is instantiated (for instance because the remote side sent a
     * message before the handshake was confirmed, this function will block untill the connection is established.
     *
     * @param msg the byte array that was received by the underlying transport socket.
     */
    void handleMessage(final byte[] msg) {
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

            final Object message;
            synchronized (this.userMessageSerializer) {
                message = this.userMessageSerializer.deserialize(msg);
            }

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
                    return;
                }
            }
            TCPConnection.log.error("[{}] - Unable to find handler method for message of type {}",
                    messageType.getSimpleName());
        } catch (final SerializationException e) {
            // Not a user-defined message, so ignore with grace!
            TCPConnection.log
                    .warn("[{}] - Received unknown message: {}. Ignoring...", this.connectionId, new String(msg));
        }
    }

    private void releaseWaitLock() {
        synchronized (this.connectionLock) {
            this.connectionLock.notifyAll();
        }
    }

    /**
     * Calling this function will wait until the Connection successfully connected, it is closed in another
     * thread, or an InterruptedException occurs. If the connection was already established finished, calling this
     * function will do nothing.
     *
     * @throws InterruptedException when the wait functions was interrupted while waiting to finish
     * @throws IOException When after the connection succeeded, the for some reason it was closed immediately before it
     *             was returned
     */
    void waitUntilConnected() throws InterruptedException, IOException {
        if (this.isConnected()) {
            return;
        }

        synchronized (this.connectionLock) {
            this.connectionLock.wait();
        }

        if (!this.isConnected()) {
            throw new ClosedChannelException();
        }
    }

    /**
     * Go to the {@link ConnectionState#CONNECTED} state. This is should be done when the HandShakeMonitor detects that
     * the remote side of the connection is also up an running. If this is the first time the connection is connected,
     * we can instantiate the appropriate ConnectionHandler from user code.
     * <p>
     * When the connection was already in the {@link ConnectionState#CONNECTED} state, nothing happens. Otherwise the
     * appropriate connection handler function is triggered, and the wait lock is released.
     *
     * @see #goToResumedState(int, String)
     */
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
            this.serviceExecutor.submit(this.serviceHandler::resumeAfterInterrupt);
            break;
        case SUSPENDED:
            this.serviceExecutor.submit(this.serviceHandler::resumeAfterSuspend);
            break;
        case TERMINATED:
        default:
            TCPConnection.log.error("[{}] - Unexpected previous state: {}", this.connectionId, this.state);
        }
        // Make sure it runs AFTER user constructor
        this.serviceExecutor.submit(this::releaseWaitLock);
    }

    /**
     * Go to the {@link ConnectionState#SUSPENDED} state. This is should be done only when the
     * {@link ConnectionManager} receives a message from the orchestrator that the connection is should be suspended.
     * When this happens the Connection will temporarily be closed, and will not attempt to reconnect until the
     * {@link #goToResumedState(int, String)} is called.
     * <p>
     * When the connection is already in the {@link ConnectionState#SUSPENDED} state, nothing happens. Otherwise the
     * heartbeat monitor is stopped, and the connection state will be updated.
     *
     * @see #goToResumedState(int, String)
     */
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

        this.serviceExecutor.submit(this.serviceHandler::onSuspend);
    }

    /**
     * Resume a connection from the {@link ConnectionState#SUSPENDED} state. This is should be done only when the
     * {@link ConnectionManager} receives a message from the orchestrator that the connection is ready to be resumed.
     * When this happens the Connection will attempt to reconnect to the remote side.
     * <p>
     * When the connection is not {@link ConnectionState#SUSPENDED} state, nothing will happen. Otherwise the connection
     * details are updated, and the socket is re-established.
     *
     * @param newListenPort the TCP port to attach to when this endpoint will act as server, or the remote address to
     *            connect to.
     * @param newTargetAddress The host name of the remote endpoint to connect to when this endpoint should act as a
     *            client, or an empty string when it should act as a server.
     * @see #goToSuspendedState()
     */
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

    /**
     * Go to the {@link ConnectionState#INTERRUPTED} state. This is typically done when the heartbeat monitor detects a
     * missed series of responses, or a read or {@link #send(Object)} operation fails. When this happens the Connection
     * will temporarily be closed, and will automatically attempt to reconnect to the remote side.
     * <p>
     * When the connection is not the {@link ConnectionState#CONNECTED} state, nothing will happen. Otherwise the
     * heartbeat monitor is stopped, and the connection state will be updated.
     */
    void goToInterruptedState() {
        if (!this.isConnected()) {
            TCPConnection.log.warn("[{}] - Not interrupting when not connected", this.connectionId);
            return;
        }

        this.state = ConnectionState.INTERRUPTED;
        if (this.serviceHandler != null) {
            this.serviceExecutor.submit(() -> {
                // It could be that in the meantime we were terminated, which means do NOT call interrupt
                if (this.state != ConnectionState.TERMINATED) {
                    this.serviceHandler.onInterrupt();
                }
            });
        }
    }

    /**
     * Go to the {@link ConnectionState#TERMINATED} state. This means that after calling this function the Connection
     * will be closed, and not able to be reinstantiated.
     *
     * @see #close()
     */
    void goToTerminatedState() {
        this.close();
    }

    @Override
    public synchronized void close() {
        // Update the state
        this.socketReader.stop();

        if (!this.state.equals(ConnectionState.TERMINATED)) {
            this.state = ConnectionState.TERMINATED;

            if (this.serviceHandler != null) {
                this.serviceExecutor.submit(this.serviceHandler::terminated);
                ConnectionManager.removeConnectionHandler(this.serviceHandler);
            }
        }

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
            TCPConnection.log.error("[{}] - Interrupted while awaiting termination", TCPConnection.this.connectionId);
        }

        this.releaseWaitLock();
    }

    private final class SocketReader implements Runnable {

        private volatile boolean keepRunning = true;

        /**
         *
         */
        SocketReader() {
            // Protected constructor for TCPConnection
        }

        @Override
        public void run() {
            while (this.keepRunning) {
                if (TCPConnection.this.socket != null) {
                    TCPConnection.log.debug("[{}] - Closing old socket", TCPConnection.this.connectionId);
                    TCPConnection.this.socket.close();

                    if (TCPConnection.this.handShakeMonitor != null) {
                        TCPConnection.this.handShakeMonitor.close();
                    }

                    if (TCPConnection.this.heartBeatMonitor != null) {
                        TCPConnection.this.heartBeatMonitor.close();
                    }
                }

                TCPConnection.log.info("[{}] - Building TCPConnection", TCPConnection.this.connectionId);
                if (TCPConnection.this.targetAddress.isEmpty()) {
                    TCPConnection.this.socket = TCPSocket.asServer(TCPConnection.this.port);
                } else {
                    TCPConnection.this.socket = TCPSocket.asClient(TCPConnection.this.targetAddress,
                            TCPConnection.this.port);
                }

                try {
                    TCPConnection.this.socket.waitUntilConnected();
                } catch (final Exception e) {
                    if (this.keepRunning) {
                        TCPConnection.log.warn("[{}] - Interrupted while waiting for connection to establish",
                                TCPConnection.this.connectionId);
                        continue;
                    } else {
                        break;
                    }
                }

                try {
                    // Create the monitors
                    TCPConnection.log.debug("[{}] - Creating connection monitors", TCPConnection.this.connectionId);
                    TCPConnection.this.handShakeMonitor = new HandShakeMonitor(TCPConnection.this.socket,
                            TCPConnection.this.connectionId);
                    TCPConnection.this.heartBeatMonitor = new HeartBeatMonitor(TCPConnection.this.socket,
                            TCPConnection.this.connectionId);
                } catch (final Exception e) {
                    if (this.keepRunning) {
                        TCPConnection.log.warn(
                                "[{}] - Exception while instantiating connection monitors. Aborting setup",
                                TCPConnection.this.connectionId);
                        continue;
                    } else {
                        break;
                    }
                }

                // Now we have a functioning socket, make sure that as soon as there is a handshake, go connected
                TCPConnection.this.connectionExecutor.submit(() -> {
                    try {
                        TCPConnection.log.debug("[{}] - Initiating handshake", TCPConnection.this.connectionId);
                        TCPConnection.this.handShakeMonitor.sendHandshake(TCPConnection.this.getState());
                        TCPConnection.this.handShakeMonitor.waitUntilFinished();
                        TCPConnection.log.debug("[{}] - Handshake confirmed, starting heartbeat",
                                TCPConnection.this.connectionId);
                        TCPConnection.this.heartBeatMonitor.start();
                        TCPConnection.this.goToConnectedState();
                    } catch (final InterruptedException e) {
                        if (this.keepRunning) {
                            TCPConnection.log.warn("[{}] - Interrupted while waiting for TCP socket to initialize",
                                    TCPConnection.this.connectionId);
                        }
                    }
                });

                while (this.keepRunning && (TCPConnection.this.socket != null)) {
                    try {
                        final byte[] data = TCPConnection.this.socket.read();

                        if ((data == null) || (data.length == 0)) {
                            continue;
                        }

                        // Check the socket again, since it may have changed since we started read()
                        if ((TCPConnection.this.socket != null)
                                && !TCPConnection.this.heartBeatMonitor.handleMessage(data)
                                && !TCPConnection.this.handShakeMonitor.handleHandShake(data)) {
                            TCPConnection.this.messageQueue.addMessage(data);
                        }
                    } catch (final IOException e) {
                        // See if this was on purpose
                        if (TCPConnection.this.isConnected() && this.keepRunning) {
                            TCPConnection.log.warn("[{}] - IOException while reading from socket: {}",
                                    TCPConnection.this.connectionId,
                                    e.getMessage());
                            TCPConnection.log.trace(e.getMessage(), e);
                            TCPConnection.this.goToInterruptedState();
                        }
                        break;
                    } catch (final Exception e) {
                        TCPConnection.log.error("[{}] - Unexpected exception while operating on socket: {}",
                                TCPConnection.this.connectionId,
                                e.getMessage());
                        TCPConnection.log.trace(e.getMessage(), e);
                        TCPConnection.this.goToInterruptedState();
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

        void stop() {
            this.keepRunning = false;
        }

    }

    private final class MessageQueue implements Runnable {

        private final BlockingQueue<byte[]> internalQueue = new LinkedBlockingQueue<>();
        private volatile boolean keepRunning = true;

        /**
         *
         */
        MessageQueue() {
            // Protected constructor for TCPConnection
        }

        void addMessage(final byte[] msg) {
            try {
                this.internalQueue.put(msg);
            } catch (final InterruptedException e) {
                TCPConnection.log.warn("[{}] - Interrupted while adding message to queue",
                        TCPConnection.this.connectionId);
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
                    TCPConnection.log.trace("[{}] - Message handler interrupted, stopping thread",
                            TCPConnection.this.connectionId);
                    break;
                }
            }
        }

        void stop() {
            this.keepRunning = false;
        }

    }

}
