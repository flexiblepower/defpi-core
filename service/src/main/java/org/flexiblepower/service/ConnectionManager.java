/**
 * File ConnectionManager.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.flexiblepower.proto.ConnectionProto.ConnectionHandshake;
import org.flexiblepower.proto.ConnectionProto.ConnectionMessage;
import org.flexiblepower.proto.ConnectionProto.ConnectionState;
import org.flexiblepower.service.exceptions.ConnectionModificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;

/**
 * ConnectionManager
 *
 * @author coenvl
 * @version 0.1
 * @since May 10, 2017
 */
/**
 * ConnectionManager
 *
 * @author coenvl
 * @version 0.1
 * @since May 18, 2017
 */
public class ConnectionManager implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(ConnectionManager.class);
    private static final Map<String, ConnectionHandlerFactory> connectionHandlers = new HashMap<>();

    private final Map<String, ManagedConnection> connections = new HashMap<>();
    private final ExecutorService executor;

    /**
     * @param serviceExecutor
     */
    public ConnectionManager(final ExecutorService serviceExecutor) {
        this.executor = serviceExecutor;
    }

    /**
     * @param parseFrom
     * @return
     * @return
     * @throws ConnectionModificationException
     */
    public Message handleConnectionMessage(final ConnectionMessage message) throws ConnectionModificationException {
        final String connectionId = message.getConnectionId();
        ConnectionManager.log
                .info("Received ConnectionMessage for connection {} ({})", connectionId, message.getMode());
        ConnectionManager.log.trace("Received message:\n{}", message);

        switch (message.getMode()) {
        case CREATE:
            return this.createConnection(connectionId, message);
        case RESUME:
            if (!this.connections.containsKey(connectionId)) {
                this.createConnection(connectionId, message);
            }
            this.connections.get(connectionId).resumeAfterSuspendedState(message.getListenPort(),
                    message.getTargetAddress());
            return ConnectionHandshake.newBuilder()
                    .setConnectionId(connectionId)
                    .setConnectionState(ConnectionState.CONNECTED)
                    .build();
        case SUSPEND:
            this.connections.get(connectionId).goToSuspendedState();
            return ConnectionHandshake.newBuilder()
                    .setConnectionId(connectionId)
                    .setConnectionState(ConnectionState.SUSPENDED)
                    .build();
        case TERMINATE:
            this.connections.remove(connectionId).goToTerminatedState();
            return ConnectionHandshake.newBuilder()
                    .setConnectionId(connectionId)
                    .setConnectionState(ConnectionState.TERMINATED)
                    .build();
        default:
            throw new ConnectionModificationException("Invalid connection modification type");
        }
    }

    /**
     * @param message
     * @throws ConnectionModificationException
     */
    private Message createConnection(final String connectionId, final ConnectionMessage message)
            throws ConnectionModificationException {
        // First find the correct handler to attach to the connection
        final String key = ConnectionManager.handlerKey(message.getReceiveHash(), message.getSendHash());
        final ConnectionHandlerFactory chf = ConnectionManager.connectionHandlers.get(key);

        if (chf == null) {
            ConnectionManager.log.error(
                    "Request for connection with unknown hashes {}, did you register service with {}.registerHandlers?",
                    key,
                    ConnectionManager.class.getSimpleName());
            throw new ConnectionModificationException("Unknown connection handling hash: " + key);
        } else {
            this.connections.put(message.getConnectionId(),
                    new ManagedConnection(message.getConnectionId(),
                            message.getListenPort(),
                            message.getTargetAddress(),
                            chf.build(),
                            this.executor));
            ConnectionManager.log.trace("Added connection {} to list", message.getConnectionId());
        }
        return ConnectionHandshake.newBuilder()
                .setConnectionId(connectionId)
                .setConnectionState(ConnectionState.STARTING)
                .build();
    }

    /**
     * @param connectionHandlerFactory
     */
    public static void registerConnectionHandlerFactory(final Class<? extends ConnectionHandler> clazz,
            final ConnectionHandlerFactory connectionHandlerFactory) {
        if (!clazz.isAnnotationPresent(InterfaceInfo.class)) {
            throw new RuntimeException(
                    "ConnectionHandler must have the InterfaceInfo annotation to be able to register");
        }
        final InterfaceInfo info = clazz.getAnnotation(InterfaceInfo.class);
        final String key = ConnectionManager.handlerKey(info.receivesHash(), info.sendsHash());
        ConnectionManager.connectionHandlers.put(key, connectionHandlerFactory);
        ConnectionManager.log.debug("Registered {} for type {}", connectionHandlerFactory, key);
    }

    private static String handlerKey(final String receivesHash, final String sendsHash) {
        return receivesHash + "/" + sendsHash;
    }

    /**
     *
     */
    @Override
    public void close() {
        for (final ManagedConnection conn : this.connections.values()) {
            conn.goToTerminatedState();
        }
        for (final ManagedConnection conn : this.connections.values()) {
            try {
                conn.waitTillFinished();
            } catch (final InterruptedException e) {
                ConnectionManager.log.warn("Interrupted while waiting for cloning connection", e);
            }
        }
    }

}
