/**
 * File ConnectionManager.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.util.HashMap;
import java.util.Map;

import org.flexiblepower.service.exceptions.ConnectionModificationException;
import org.flexiblepower.service.proto.ServiceProto.ConnectionMessage;
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
public class ConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(ConnectionManager.class);
    private static final Map<String, ConnectionHandlerFactory> connectionHandlers = new HashMap<>();

    private final Map<String, ManagedConnection> connections = new HashMap<>();

    /**
     * @param parseFrom
     * @return
     * @throws ConnectionModificationException
     */
    public void handleConnectionMessage(final ConnectionMessage message) throws ConnectionModificationException {
        ConnectionManager.log.info("Received ConnectionMessage for connection {} ({})",
                message.getConnectionId(),
                message.getMode());
        ConnectionManager.log.trace("Received message: {}", message);

        final String id = message.getConnectionId();
        switch (message.getMode()) {
        case CREATE:
            this.createConnection(message);
            break;
        case RESUME:
            this.connections.get(id).resume();
            break;
        case SUSPEND:
            this.connections.get(id).suspend();
            break;
        case TERMINATE:
            this.connections.remove(id).close();
            break;
        default:
            throw new ConnectionModificationException("Invalid connection modification type");
        }
    }

    /**
     * @param message
     * @throws ConnectionModificationException
     */
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
                    new ManagedConnection(message.getListenPort(), message.getTargetAddress(), chf.build()));
        }
    }

    /**
     * @param connectionHandlerFactory
     */
    public static void registerHandlers(final Class<? extends ConnectionHandler> clazz,
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
    public void close() {
        for (final ManagedConnection conn : this.connections.values()) {
            conn.close();
        }
    }

}
