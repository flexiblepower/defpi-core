/**
 * File ConnectionManager.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

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
    private static final Map<String, ConnectionHandlerManager> connectionHandlers = new HashMap<>();
    private static final Map<String, InterfaceInfo> interfaceInfo = new HashMap<>();

    private final Map<String, TCPConnection> connections = new HashMap<>();

    /**
     * @param parseFrom
     * @return
     * @return
     * @throws ConnectionModificationException
     */
    public Message handleConnectionMessage(final ConnectionMessage message) throws IOException,
            ConnectionModificationException {
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
            this.connections.get(connectionId).goToResumedState(message.getListenPort(), message.getTargetAddress());
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
     * @throws IOException
     */
    private Message createConnection(final String connectionId, final ConnectionMessage message)
            throws ConnectionModificationException,
            IOException {
        // First find the correct handler to attach to the connection
        final String key = ConnectionManager.handlerKey(message.getReceiveHash(), message.getSendHash());
        final ConnectionHandlerManager chf = ConnectionManager.connectionHandlers.get(key);
        final InterfaceInfo info = ConnectionManager.interfaceInfo.get(key);

        if ((chf == null) || (info == null)) {
            ConnectionManager.log.error(
                    "Request for connection with unknown hashes {}, did you register service with {}.registerHandlers?",
                    key,
                    ConnectionManager.class.getSimpleName());
            throw new ConnectionModificationException("Unknown connection handling hash: " + key);
        } else {
            @SuppressWarnings("resource")
            final TCPConnection conn = new TCPConnection(message.getConnectionId(),
                    message.getListenPort(),
                    message.getTargetAddress(),
                    info,
                    message.getOtherProcessId());
            this.connections.put(message.getConnectionId(), conn);
        }
        return ConnectionHandshake.newBuilder()
                .setConnectionId(connectionId)
                .setConnectionState(ConnectionState.STARTING)
                .build();

    }

    static ConnectionHandler buildHandlerForConnection(final Connection c, final InterfaceInfo info) {
        final String key = ConnectionManager.handlerKey(info.receivesHash(), info.sendsHash());
        final ConnectionHandlerManager chf = ConnectionManager.connectionHandlers.get(key);

        final String methodName = "build" + ConnectionManager.camelCaps(info.version());

        try {
            final Method buildMethod = chf.getClass().getMethod(methodName, Connection.class);
            return (ConnectionHandler) buildMethod.invoke(chf, c);
        } catch (final Exception e) {
            throw new RuntimeException("Error building connection handler: " + e.getMessage(), e);
        }
    }

    /**
     * @param connectionHandlerManager
     */
    public static void registerConnectionHandlerFactory(final Class<? extends ConnectionHandler> clazz,
            final ConnectionHandlerManager connectionHandlerManager) {
        if (!clazz.isAnnotationPresent(InterfaceInfo.class)) {
            throw new RuntimeException(
                    "ConnectionHandler must have the InterfaceInfo annotation to be able to register");
        }
        final InterfaceInfo info = clazz.getAnnotation(InterfaceInfo.class);
        final String key = ConnectionManager.handlerKey(info.receivesHash(), info.sendsHash());

        ConnectionManager.connectionHandlers.put(key, connectionHandlerManager);
        ConnectionManager.interfaceInfo.put(key, info);
        ConnectionManager.log.debug("Registered {} for type {}", connectionHandlerManager, key);
    }

    private static String handlerKey(final String receivesHash, final String sendsHash) {
        return receivesHash + "/" + sendsHash;
    }

    private static String camelCaps(final String str) {
        final StringBuilder ret = new StringBuilder();

        for (final String word : str.split(" ")) {
            if (!word.isEmpty()) {
                ret.append(Character.toUpperCase(word.charAt(0)));
                ret.append(word.substring(1).toLowerCase());
            }
        }

        // Return a cleaned-up string
        return ret.toString().replaceAll("[^a-zA-Z0-9_]", "");
    }

    /**
     *
     */
    @Override
    public void close() {
        for (final TCPConnection conn : this.connections.values()) {
            conn.goToTerminatedState();
        }
        // for (final ManagedConnection conn : this.connections.values()) {
        // try {
        // conn.waitTillFinished();
        // } catch (final InterruptedException e) {
        // ConnectionManager.log.warn("Interrupted while waiting for cloning connection", e);
        // }
        // }
    }

}
