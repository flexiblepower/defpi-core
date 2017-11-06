/**
 * File ConnectionMonitor.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.io.IOException;

import org.flexiblepower.commons.TCPSocket;
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

    private final String connectionId;
    private final TCPSocket socket;
    private final ProtobufMessageSerializer serializer;

    private final Object waitLock = new Object();
    private boolean ready;

    public HandShakeMonitor(final TCPSocket socket, final String connectionId) {
        this.socket = socket;
        this.connectionId = connectionId;
        this.ready = false;

        // Add Protobuf serializer for ConnectionHandshake messages
        this.serializer = new ProtobufMessageSerializer();
        this.serializer.addMessageClass(ConnectionHandshake.class);
    }

    public void sendHandshake(final ConnectionState currentState) {
        // Send the handshake
        final ConnectionHandshake initHandshakeMessage = ConnectionHandshake.newBuilder()
                .setConnectionId(this.connectionId)
                .setConnectionState(currentState)
                .build();
        HandShakeMonitor.log.trace("[{}] - Sending handshake {}", this.connectionId, currentState);
        try {
            this.socket.send(this.serializer.serialize(initHandshakeMessage));
        } catch (final SerializationException e) {
            // This should not happen
            throw new RuntimeException("Exception while serializing message: " + initHandshakeMessage, e);
        } catch (final InterruptedException | IOException e) {
            this.close();
        }
    }

    public boolean handleHandShake(final byte[] recvData) {
        // Receive the HandShake
        ConnectionHandshake handShakeMessage = null;

        try {
            handShakeMessage = (ConnectionHandshake) this.serializer.deserialize(recvData);
        } catch (final SerializationException e) {
            // It is not a handshake
            return false;
        }

        if (handShakeMessage.getConnectionId().equals(this.connectionId)) {
            HandShakeMonitor.log.debug("[{}] - Received acknowledgement: {}",
                    this.connectionId,
                    handShakeMessage.getConnectionState());
            // Success! Send response back, or we are finished
            if (!this.ready || !handShakeMessage.getConnectionState().equals(ConnectionState.CONNECTED)) {
                // This is the handshake that will make the other guy READY
                this.sendHandshake(ConnectionState.CONNECTED);
            } else {
                HandShakeMonitor.log.info("[{}] - Not responding to handshake", this.connectionId);
            }

            if (handShakeMessage.getConnectionState().equals(ConnectionState.CONNECTED)) {
                HandShakeMonitor.log.info("[{}] - Received connection confirmation, we are ready and release waitLock",
                        this.connectionId);
                this.ready = true;
                this.releaseWaitLock();
            }

            return true;
        } else {
            HandShakeMonitor.log.warn("[{}] - Invalid Connection ID in Handshake message: {}",
                    this.connectionId,
                    handShakeMessage.getConnectionId());
            return true;
        }

    }

    public boolean ready() {
        return this.ready;
    }

    public void waitUntilFinished(final long millis) throws InterruptedException {
        if (this.ready()) {
            return;
        } else {
            synchronized (this.waitLock) {
                if (millis < 1) {
                    this.waitLock.wait();
                } else {
                    this.waitLock.wait(millis);
                }
            }
        }
    }

    private void releaseWaitLock() {
        synchronized (this.waitLock) {
            this.waitLock.notifyAll();
        }
    }

    public void close() {
        this.releaseWaitLock();
        this.socket.close();
    }

}
