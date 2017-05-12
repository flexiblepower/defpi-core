/**
 * File ConnectionManager.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
public class ConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(ConnectionManager.class);
    private final Map<String, ManagedConnection> connections = new HashMap<>();
    private final Service service;
    private final Set<MessageHandlerWrapper> messageHandlers;

    /**
     * @param service
     * @param messagehandlers
     */
    public ConnectionManager(final Service service, final Set<MessageHandlerWrapper> messageHandlers) {
        this.service = service;
        this.messageHandlers = messageHandlers;
    }

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
        final MessageHandlerWrapper handler = this.getMessageHandler(message.getReceiveHash());

        final ManagedConnection connection = new ManagedConnection(message.getListenPort(), message.getTargetAddress());
        this.connections.put(message.getConnectionId(), connection);
        connection.addHandler(handler);
    }

    /**
     * @param receiveHash
     * @throws ConnectionModificationException
     */
    private MessageHandlerWrapper getMessageHandler(final String receiveHash) throws ConnectionModificationException {

        for (final MessageHandlerWrapper mhw : this.messageHandlers) {
            if (mhw.getHandlesHash().equals(receiveHash)) {
                return mhw;
            }
        }
        throw new ConnectionModificationException("Unknown handling hash: " + receiveHash);
    }

}
