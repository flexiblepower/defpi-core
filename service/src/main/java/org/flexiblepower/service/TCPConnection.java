/**
 * File TCPConnection.java
 *
 * Copyright 2017 FAN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    protected final ExecutorService connectionExecutor = Executors.newFixedThreadPool(3,
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

    /**
     * @throws ClosedChannelException when the state is not connected, i.e. when {@link #isConnected()} returns false.
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
            data = this.userMessageSerializer.serialize(message);
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
            this.serviceExecutor.submit(() -> {
                // It could be that in the meantime we were terminated, which means do NOT call interrupt
                if (this.state != ConnectionState.TERMINATED) {
                    this.serviceHandler.onInterrupt();
                }
            });
        }
    }

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
                this.serviceExecutor.submit(() -> this.serviceHandler.terminated());
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
        protected SocketReader() {
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

                // Create the monitors
                TCPConnection.log.debug("[{}] - Creating connection monitors", TCPConnection.this.connectionId);
                TCPConnection.this.handShakeMonitor = new HandShakeMonitor(TCPConnection.this.socket,
                        TCPConnection.this.connectionId);
                TCPConnection.this.heartBeatMonitor = new HeartBeatMonitor(TCPConnection.this.socket,
                        TCPConnection.this.connectionId);

                // Now we have a functioning socket, make sure that as soon as there is a handshake, go connected
                TCPConnection.this.connectionExecutor.submit(() -> {
                    try {
                        TCPConnection.log.debug("[{}] - Initiating handshake", TCPConnection.this.connectionId);
                        TCPConnection.this.handShakeMonitor.sendHandshake(TCPConnection.this.getState());
                        TCPConnection.this.handShakeMonitor.waitUntilFinished(0);
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

        public void stop() {
            this.keepRunning = false;
        }

    }

}
