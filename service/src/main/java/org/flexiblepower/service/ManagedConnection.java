/**
 * File ManagedConnection.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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

    private ConnectionState state;
    private volatile boolean keepThreadAlive;

    private final Context zmqContext;
    private final Socket subscribeSocket;
    private final Socket publishSocket;
    private final Thread connectionThread;

    private final ConnectionHandler handler;
    private final MessageSerializer<Object> javaIoSerializer;
    private final ProtobufMessageSerializer<ConnectionHandshake> protoBufSerializer;

    private InterfaceInfo info = null;

    private final String connectionId;

    private ConnectionHandshake initHandshakeMessage;

    /**
     * @param targetAddress
     * @param listenPort
     * @throws ConnectionModificationException
     * @throws SerializationException
     *
     */
    ManagedConnection(final String connectionId,
            final int listenPort,
            final String targetAddress,
            final ConnectionHandler handler) throws ConnectionModificationException {
        this.connectionId = connectionId;
        this.handler = handler;
        this.info = ManagedConnection.getInfoFromHandler(handler);
        this.protoBufSerializer = new ProtobufMessageSerializer<>();
        this.protoBufSerializer.addMessageClass(ConnectionHandshake.class);

        // Add serializer to the connection
        try {
            this.javaIoSerializer = this.info.serializer().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ConnectionModificationException("Unable to serializer instantiate connection");
        }

        for (final Class<?> messageType : this.info.receiveTypes()) {
            this.javaIoSerializer.addMessageClass(messageType);
        }

        this.state = ConnectionState.STARTING;
        this.zmqContext = ZMQ.context(1);
        this.publishSocket = this.zmqContext.socket(ZMQ.PUSH);
        this.subscribeSocket = this.zmqContext.socket(ZMQ.PULL);

        // this.connectionThread = new Thread(() -> {

        ManagedConnection.log.debug("Creating publishSocket to {}", targetAddress);
        this.publishSocket.setSendTimeOut(0); // ManagedConnection.SEND_TIMEOUT);
        this.publishSocket.setDelayAttachOnConnect(true);
        this.publishSocket.connect(targetAddress);

        final String listenAddress = "tcp://*:" + listenPort;
        ManagedConnection.log.debug("Creating subscribeSocket listening on port {}", listenAddress);
        this.subscribeSocket.setReceiveTimeOut(ManagedConnection.RECEIVE_TIMEOUT);
        this.subscribeSocket.bind(listenAddress);

        if (this.publishSocket.send(this.initialiseHandshake())) {
            ManagedConnection.log.debug("Succesfully sent Handshake to " + targetAddress);
        } else {
            ManagedConnection.log.debug("Failed to send Handshake to " + targetAddress);
        }

        this.keepThreadAlive = true;

        this.connectionThread = new Thread(() -> {
            while (this.keepThreadAlive) {
                try {
                    this.handleByteArray(this.subscribeSocket.recv());
                } catch (final ZMQException e) {
                    if (e.getErrorCode() == 156384765) {
                        ManagedConnection.log.info("Socket closed, stopping thread");
                        break;
                    }
                } catch (final Exception e) {
                    ManagedConnection.log.error("Exception handling message: {}", e.getMessage());
                    ManagedConnection.log.trace("Exception handing message", e);
                }
            }
            ManagedConnection.log.trace("End of thread");
        }, "Managed " + this.info.name() + " handler thread");

        this.connectionThread.start();
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
     * @return a special series of bytes that indicate this connection is ready.
     * @throws SerializationException
     */
    private byte[] initialiseHandshake() {
        this.initHandshakeMessage = ConnectionHandshake.newBuilder()
                .setConnectionId(this.connectionId)
                .setConnectionState(ConnectionState.STARTING)
                .build();

        return this.protoBufSerializer.serialize(this.initHandshakeMessage);
    }

    private void acknowledgeHandshake(final ConnectionHandshake message) {
        ManagedConnection.log.debug("Received acknowledge string: {}", message);

        this.state = ConnectionState.CONNECTED;
        this.handler.onConnected(this);

        ManagedConnection.log.debug("Updated state to {}, replying ack", this.state);

        final ConnectionHandshake response = ConnectionHandshake.newBuilder()
                .setConnectionId(this.connectionId)
                .setConnectionState(ConnectionState.CONNECTED)
                .build();

        this.publishSocket.send(this.protoBufSerializer.serialize(response));
    }

    /**
     * @param buff
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws SerializationException
     */
    private void handleByteArray(final byte[] buff)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (buff == null) {
            return;
        }

        try {
            final ConnectionHandshake handShakeMessage = this.protoBufSerializer.deserialize(buff);
            if ((this.state == ConnectionState.STARTING)
                    && (this.connectionId.equals(handShakeMessage.getConnectionId()))) {
                this.acknowledgeHandshake(handShakeMessage);
            }
            return;
        } catch (final SerializationException e) {
            // Not a handshake but a service-implemented message, so ignore!
        }

        try {
            final Object message = this.javaIoSerializer.deserialize(buff);

            final Class<?> messageType = message.getClass();
            final Method[] allMethods = this.handler.getClass().getMethods();
            for (final Method method : allMethods) {
                if ((method.getName().startsWith("handle")) && (method.getName().endsWith("Message"))
                        && (method.getParameterCount() == 1) && method.getParameterTypes()[0].equals(messageType)) {
                    method.invoke(this.handler, message);
                }
            }
        } catch (final SerializationException e) {
            // Not a service-implemented message either, so ignore again!
            ManagedConnection.log.warn("Received unknown message : " + new String(buff) + ". Ignoring...");
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
                this.publishSocket.send(this.javaIoSerializer.serialize(message));
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
            this.zmqContext.close();
        }
    }

}
