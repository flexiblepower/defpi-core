/**
 * File ConnectionManager.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

import org.flexiblepower.proto.ConnectionProto.ConnectionHandshake;
import org.flexiblepower.proto.ConnectionProto.ConnectionHandshake.Builder;
import org.flexiblepower.proto.ConnectionProto.ConnectionMessage;
import org.flexiblepower.proto.ConnectionProto.ConnectionState;
import org.flexiblepower.service.exceptions.ConnectionModificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /**
     * @param parseFrom
     * @return
     * @return
     * @throws ConnectionModificationException
     */
    public ConnectionHandshake handleConnectionMessage(final ConnectionMessage message)
            throws ConnectionModificationException {
        final String connectionId = message.getConnectionId();
        ConnectionManager.log
                .info("Received ConnectionMessage for connection {} ({})", connectionId, message.getMode());
        ConnectionManager.log.trace("Received message:\n{}", message);
        final Builder responseBuilder = ConnectionHandshake.newBuilder().setConnectionId(connectionId);

        switch (message.getMode()) {
        case CREATE:
            this.createConnection(message);
            responseBuilder.setConnectionState(ConnectionState.STARTING);
            break;
        case RESUME:
            this.connections.get(connectionId).resume();
            responseBuilder.setConnectionState(ConnectionState.CONNECTED);
            break;
        case SUSPEND:
            this.connections.get(connectionId).suspend();
            responseBuilder.setConnectionState(ConnectionState.SUSPENDED);
            break;
        case TERMINATE:
            this.connections.remove(connectionId).close();
            responseBuilder.setConnectionState(ConnectionState.TERMINATED);
            break;
        default:
            throw new ConnectionModificationException("Invalid connection modification type");
        }
        return responseBuilder.build();
    }

    /**
     * @param message
     * @throws ConnectionModificationException
     */
    @SuppressWarnings("resource")
    private void createConnection(final ConnectionMessage message) throws ConnectionModificationException {
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
                            chf.build()));
            ConnectionManager.log.trace("Added connection {} to list", message.getConnectionId());
        }
    }

    /**
     * @param connectionHandlerFactory
     */
    public static void registerConnectionHandlerFactory(final Class<? extends ConnectionHandler> clazz,
            final ConnectionHandlerFactory connectionHandlerFactory) {
        if (!clazz.isAnnotationPresent(InterfaceInfo.class)) {
            throw new RuntimeException(
                    "ConnectionHandlerFactory must have the InterfaceInfo annotation to be able to register");
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
            conn.close();
        }
    }

}
