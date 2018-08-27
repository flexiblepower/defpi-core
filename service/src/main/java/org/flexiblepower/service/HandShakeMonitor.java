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
import java.io.IOException;

import org.flexiblepower.commons.TCPSocket;
import org.flexiblepower.exceptions.SerializationException;
import org.flexiblepower.proto.ConnectionProto.ConnectionHandshake;
import org.flexiblepower.proto.ConnectionProto.ConnectionState;
import org.flexiblepower.serializers.ProtobufMessageSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The handshake monitor adds functionality to a socket to ensure the correct initialization when the socket is started.
 * When the HandShakeMonitor is built, it will send a HandShake object to the remote side, containing the locally known
 * connectionId. On the remote side, a handshake monitor will use this to check if they have the same id.
 * <p>
 * When both sides acknowledge that they were able to receive and send handshakes, and that both connections are in
 * the {@link ConnectionState#CONNECTED} state, the waitLock is released and the handshake monitor is considered to be
 * finished.
 *
 * @version 0.1
 * @since Aug 23, 2017
 */
public class HandShakeMonitor implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(HandShakeMonitor.class);

    private final String connectionId;
    private final TCPSocket socket;
    private final ProtobufMessageSerializer serializer;

    private final Object waitLock = new Object();
    private boolean ready;

    /**
     * Create a HandShakeMonitor for the specified socket.
     *
     * @param socket The socket to perform the handshake on
     * @param connectionId The id of the connection to identify the connection
     */
    HandShakeMonitor(final TCPSocket socket, final String connectionId) {
        this.socket = socket;
        this.connectionId = connectionId;
        this.ready = false;

        // Add Protobuf serializer for ConnectionHandshake messages
        this.serializer = new ProtobufMessageSerializer();
        this.serializer.addMessageClass(ConnectionHandshake.class);
    }

    /**
     * Send a handshake object to the remote side. This handshake will contain information about the connectionId that
     * was set in the constructor, and the current connection state.
     *
     * @param currentState the ConnectionState of the local connection endpoint
     */
    void sendHandshake(final ConnectionState currentState) {
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
        } catch (final IOException e) {
            this.close();
        }
    }

    /**
     * Interpret a byte array as a incoming handshake object. This object might be a response to a handshake this
     * monitor sent earlier; if so, this function will return true, and false otherwise.
     *
     * @param recvData incoming byte array that may or may not be a response to our handshake
     * @return true iff the byte array is a valid response
     */
    boolean handleHandShake(final byte[] recvData) {
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

    /**
     * @return Whether this monitor object has acknowledged the remote side of the connection is instantiated.
     */
    boolean ready() {
        return this.ready;
    }

    /**
     * Calling this function will wait until the handshake monitor successfully connected, it is closed in another
     * thread, or an InterruptedException occurs. If the handshake monitor was already finished, calling this function
     * will do nothing.
     *
     * @throws InterruptedException when the wait functions was interrupted while waiting to finish
     * @see #close()
     */
    void waitUntilFinished() throws InterruptedException {
        if (this.ready()) {
            return;
        } else {
            synchronized (this.waitLock) {
                this.waitLock.wait();
            }
        }
    }

    private void releaseWaitLock() {
        synchronized (this.waitLock) {
            this.waitLock.notifyAll();
        }
    }

    /**
     * Closes the handshake monitor, releasing any threads that were waiting for it to finish, and also close the socket
     * that it was attached to.
     */
    @Override
    public void close() {
        this.releaseWaitLock();
        this.socket.close();
    }

}
