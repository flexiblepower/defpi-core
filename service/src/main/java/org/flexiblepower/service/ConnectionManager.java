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
 * The connection manager is the object that is responsible for maintaining all connections to and from a process. It is
 * used by the ServiceManager to handler all ConnectionMessages.
 *
 * @version 0.1
 * @since May 10, 2017
 */
public final class ConnectionManager implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(ConnectionManager.class);
    private static final Map<String, ConnectionHandlerManager> connectionBuilders = new HashMap<>();
    private static final Map<String, InterfaceInfo> interfaceInfo = new HashMap<>();
    private static final Map<ConnectionHandler, Connection> handlerConnectionMap = new HashMap<>();

    private final Map<String, TCPConnection> connections = new HashMap<>();

    /**
     * Handles the connection message to either create, resume, suspend or terminate a connection.
     *
     * @param message the message that contains the instruction to perform
     * @return The resulting confirmation message containing the new connection state
     * @throws ConnectionModificationException When an error occurs while creating or updating the connection
     */
    Message handleConnectionMessage(final ConnectionMessage message) throws ConnectionModificationException {
        final String connectionId = message.getConnectionId();
        ConnectionManager.log
                .info("Received ConnectionMessage for connection {} ({})", connectionId, message.getMode());
        ConnectionManager.log.trace("Received message:\n{}", message);

        switch (message.getMode()) {
        case CREATE:
            if (!this.connections.containsKey(connectionId)) {
                return this.createConnection(message);
            } else {
                ConnectionManager.log.info("Ignore create-message for already existing connection {}", connectionId);
                return ConnectionHandshake.newBuilder()
                        .setConnectionId(connectionId)
                        .setConnectionState(ConnectionState.CONNECTED)
                        .build();
            }
        case RESUME:
            if (!this.connections.containsKey(connectionId)) {
                this.createConnection(message);
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
     * @param message A ConnectionMessage describing the connection to build
     * @throws ConnectionModificationException When the service is unable to build / modify the connection
     */
    private Message createConnection(final ConnectionMessage message) throws ConnectionModificationException {
        // First find the correct handler to attach to the connection
        final String key = ConnectionManager.handlerKey(message.getReceiveHash(), message.getSendHash());
        final ConnectionHandlerManager chf = ConnectionManager.connectionBuilders.get(key);
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
                    message.getRemoteProcessId(),
                    message.getRemoteServiceId(),
                    message.getRemoteInterfaceId());
            this.connections.put(message.getConnectionId(), conn);
        }
        return ConnectionHandshake.newBuilder()
                .setConnectionId(message.getConnectionId())
                .setConnectionState(ConnectionState.STARTING)
                .build();

    }

    /**
     * Build the handler object for the connection and the specified interface info. This function will use reflection
     * to get the class from user code that implements the ConnectionHandler. It does so by invoking the builder
     * function in the ConnectionHandlerManager also from the user code.
     * <p>
     * In order for the lookup to work, the corresponding ConnectionHandler and ConnectionHandlerManager must be
     * registered using the {@link #registerConnectionHandlerFactory(Class, ConnectionHandlerManager)} function.
     *
     * @param c The connection object to build a connection handler for
     * @param info The information of the interface, from which we get the info about the type of handler to build
     * @return The connection handler built for this connection
     */
    static ConnectionHandler buildHandlerForConnection(final Connection c, final InterfaceInfo info) {
        final String key = ConnectionManager.handlerKey(info.receivesHash(), info.sendsHash());
        final ConnectionHandlerManager chf = ConnectionManager.connectionBuilders.get(key);

        final String methodName = "build" + ConnectionManager.camelCaps(info.version());

        try {
            final Method buildMethod = chf.getClass().getMethod(methodName, Connection.class);
            final ConnectionHandler handler = (ConnectionHandler) buildMethod.invoke(chf, c);
            ConnectionManager.handlerConnectionMap.put(handler, c);
            return handler;
        } catch (final Exception e) {
            throw new RuntimeException("Error building connection handler: " + e.getMessage(), e);
        }
    }

    /**
     * Register a ConnectionHandlerManager as a factory for a specific type of ConnectionHandler. This means that in the
     * future whenever the specified type of ConnectionHandler is required, the object manager is used to build it.
     *
     * @param clazz The type of ConnectionHandler to register. This class must have the {@link InterfaceInfo}
     *            annotation.
     * @param connectionHandlerManager The object that is capable of building the ConnectionHandler
     * @throws RuntimeException when the clazz argument class is not annotated with InterfaceInfo
     */
    static void registerConnectionHandlerFactory(final Class<? extends ConnectionHandler> clazz,
            final ConnectionHandlerManager connectionHandlerManager) {
        if (!clazz.isAnnotationPresent(InterfaceInfo.class)) {
            throw new RuntimeException(
                    "ConnectionHandler must have the InterfaceInfo annotation to be able to register");
        }
        final InterfaceInfo info = clazz.getAnnotation(InterfaceInfo.class);
        final String key = ConnectionManager.handlerKey(info.receivesHash(), info.sendsHash());

        ConnectionManager.connectionBuilders.put(key, connectionHandlerManager);
        ConnectionManager.interfaceInfo.put(key, info);
        ConnectionManager.log.debug("Registered {} for type {}", connectionHandlerManager, key);
    }

    /**
     * Remove a known handler from the handler map;
     *
     * @param handler the handler to remove
     */
    static void removeConnectionHandler(final ConnectionHandler handler) {
        ConnectionManager.handlerConnectionMap.remove(handler);
    }

    /**
     * This is a friendly way to get the connection of a connection handler. Handlers should have received their
     * connection in their constructor, but for some services it is nice to get it via a central registry, which is
     * provided with this function.
     *
     * @param handler the connection handler to find the connection of
     * @return The connection which is handled by this handler
     */
    public static Connection getMyConnection(final ConnectionHandler handler) {
        return ConnectionManager.handlerConnectionMap.get(handler);
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
    }

}
