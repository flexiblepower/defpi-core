/**
 * File ConnectionMonitor.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import org.flexiblepower.exceptions.SerializationException;
import org.flexiblepower.proto.ConnectionProto.ConnectionHandshake;
import org.flexiblepower.proto.ConnectionProto.ConnectionState;
import org.flexiblepower.serializers.ProtobufMessageSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConnectionMonitor
 *
 * @author coenvl
 * @version 0.1
 * @since Aug 23, 2017
 */
public class HandShakeMonitor {

    private static final Logger log = LoggerFactory.getLogger(HandShakeMonitor.class);
    // private static final int MAX_RECEIVE_TRIES = 100;

    // private final Socket publishSocket;
    // private final Socket subscribeSocket;
    private final String connectionId;
    private final ManagedConnection connection;
    private final ProtobufMessageSerializer serializer;

    public HandShakeMonitor(final ManagedConnection connection, final String connectionId) {
        // final Socket publishSocket, final String connectionId) {
        // , final Socket subscribeSocket, final String connectionId) {
        // this.publishSocket = this.publishSocket;
        // this.subscribeSocket = subscribeSocket;
        this.connection = connection;
        this.connectionId = connectionId;

        // Add Protobuf serializer for ConnectionHandshake messages
        this.serializer = new ProtobufMessageSerializer();
        this.serializer.addMessageClass(ConnectionHandshake.class);
    }

    public boolean sendHandshake(final ConnectionState currentState) {
        // Send the handshake
        final ConnectionHandshake initHandshakeMessage = ConnectionHandshake.newBuilder()
                .setConnectionId(this.connectionId)
                .setConnectionState(currentState)
                .build();

        final byte[] sendData;
        try {
            sendData = this.serializer.serialize(initHandshakeMessage);
        } catch (final SerializationException e) {
            // This should not happen
            throw new RuntimeException("Exception while serializing message: " + initHandshakeMessage, e);
        }

        if (!this.connection.sendRaw(sendData)) {
            // Failed sending handshake
            HandShakeMonitor.log.warn("Failed to send handshake");
            return false;
        }

        return true;
    }

    public boolean receiveHandShake(final byte[] recvData) {
        // Receive the HandShake
        try {
            /*
             * ManagedConnection.log.trace("Listening for handshake..");
             * final byte[] recvData = this.subscribeSocket.recv();
             *
             * if (recvData == null) {
             * // Timeout occured, try again
             * return false;
             * }
             */

            final ConnectionHandshake handShakeMessage = (ConnectionHandshake) this.serializer.deserialize(recvData);

            if (handShakeMessage.getConnectionId().equals(this.connectionId)) {
                ManagedConnection.log.debug("Received acknowledge string: {}", handShakeMessage);
                // We are done
                this.connection.goToConnectedState();
                return true;
            } else {
                ManagedConnection.log
                        .warn("Invalid Connection ID in Handshake message : " + handShakeMessage.getConnectionId());
                return false;
            }
        } catch (final SerializationException e) {
            // Maybe it was a handshake?
            // HandShakeMonitor.log.warn("Received unexpected message while listening for handshake: {}",
            // e.getMessage());
            // HandShakeMonitor.log.trace(e.getMessage(), e);
            return false;
        } catch (final Exception e) {
            // The subscribeSocket is closed, probably the session was suspended before it was running
            ManagedConnection.log.warn("Exception while receiving from socket: {}", e.getMessage());
            HandShakeMonitor.log.trace(e.getMessage(), e);
            return false;
        }
    }

}
