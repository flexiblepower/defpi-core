/**
 * File ConnectionManager.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.flexiblepower.proto.ConnectionProto.ConnectionHandshake;
import org.flexiblepower.proto.ConnectionProto.ConnectionMessage;
import org.flexiblepower.proto.ConnectionProto.ConnectionState;
import org.flexiblepower.proto.ServiceProto.ErrorMessage;
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
    private static final long CONN_INVOKE_TIMEOUT_SECONDS = 1;
    private static final TimeUnit SECONDS = TimeUnit.SECONDS;

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
            return this.connections.get(connectionId).resume();
        case SUSPEND:
            return this.connections.get(connectionId).suspend();
        case TERMINATE:
            final Future<ConnectionHandshake> future = this.executor.submit(() -> {
                this.connections.remove(connectionId).close();
                return ConnectionHandshake.newBuilder()
                        .setConnectionId(connectionId)
                        .setConnectionState(ConnectionState.TERMINATED)
                        .build();
            });
            try {
                return future.get(ConnectionManager.CONN_INVOKE_TIMEOUT_SECONDS, ConnectionManager.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                return ErrorMessage.newBuilder().setDebugInformation(e.getMessage()).setProcessId(connectionId).build();
            }
        default:
            throw new ConnectionModificationException("Invalid connection modification type");
        }
    }

    /**
     * @param message
     * @throws ConnectionModificationException
     */
    @SuppressWarnings("resource")
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
            conn.close();
        }
    }

}
