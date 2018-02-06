/**
 * File HeartBeatMonitor.java
 *
 * Copyright 2017 FAN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flexiblepower.service;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.flexiblepower.commons.TCPSocket;
import org.flexiblepower.proto.ConnectionProto.ConnectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The heartbeat monitor adds functionality to a socket by periodically sending a heartbeat PING byte. A remote socket
 * with a heartbeat monitor will respond to it with a PONG byte. This PONG signifies that the connection is still alive,
 * and hence multiple missed PONGs will lead to the monitor to conclude that the connection is interrupted.
 *
 * @version 0.1
 * @since Aug 23, 2017
 */
public class HeartBeatMonitor implements Closeable {

    private final static Logger log = LoggerFactory.getLogger(HeartBeatMonitor.class);

    private static final long HEARTBEAT_PERIOD_IN_SECONDS = 10;
    private static final long HEARTBEAT_INITIAL_DELAY = 1;
    private static final TimeUnit HEARTBEAT_TIMING_UNIT = TimeUnit.SECONDS;

    private static final byte[] PING = new byte[] {(byte) 0xA};
    private static final byte[] PONG = new byte[] {(byte) 0xB};

    private static final int MAX_MISSED_HEARTBEATS = 2;

    private static int threadCount = 0;

    private final TCPSocket socket;
    private final String connectionId;
    private final ScheduledExecutorService executor;

    private ScheduledFuture<?> heartBeatFuture;
    private boolean receivedPong;
    private int missedHeartBeats;

    /**
     * Create a HeartBeatMonitor for the specified socket.
     *
     * @param socket The socket to perform the heartbeat on
     * @param connectionId The id of the connection, mostly used for logging.
     */
    public HeartBeatMonitor(final TCPSocket socket, final String connectionId) {
        final ThreadFactory threadFactory = r -> new Thread(r, "dEF-Pi hbMonThread-" + HeartBeatMonitor.threadCount++);
        this.executor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        this.socket = socket;
        this.connectionId = connectionId;
    }

    /**
     * Handle a byte array to see if it is a heartbeat. If it is a ping, it will send back a pong, if it was a pong it
     * will take note that the other side of the connection is alive.
     *
     * @param data
     * @return whether the data was actually a heartbeat or not
     */
    boolean handleMessage(final byte[] data) {
        if (data.length != HeartBeatMonitor.PING.length) {
            return false;
        }

        // If message is only 1 byte long, it is probably a heartbeat
        if (Arrays.equals(data, HeartBeatMonitor.PONG)) {
            // If ponged, it is a response to our ping
            // HeartBeatMonitor.log.trace("[{}] - -> PONG", this.connectionId);
            this.receivedPong = true;
            this.missedHeartBeats = 0;
            return true;
        } else if (Arrays.equals(data, HeartBeatMonitor.PING)) {
            // If pinged, respond with a pong
            // HeartBeatMonitor.log.trace("[{}] - PING -> PONG", this.connectionId);
            try {
                this.socket.send(HeartBeatMonitor.PONG);
            } catch (final Exception e) {
                HeartBeatMonitor.log.warn("[{}] - Unable to reply heartbeat, closing socket", this.connectionId);
                this.socket.close();
            }
            return true;
        } else {
            return false;
        }

    }

    /**
     * Start the heartbeat thread that will periodically send a ping, and check if it has received a pong before the
     * next cycle.
     */
    void start() {
        if (this.heartBeatFuture != null) {
            this.heartBeatFuture.cancel(true);
        }

        this.receivedPong = true;
        this.missedHeartBeats = 0;
        this.heartBeatFuture = this.executor.scheduleAtFixedRate(() -> {
            try {
                if (!this.socket.isConnected()) {
                    this.close();
                    return;
                }

                if (!this.receivedPong) {
                    // If no PONG was received since the last PING, assume connection was interrupted!
                    HeartBeatMonitor.log
                            .warn("[{}] - Missed a heartbeat...", this.connectionId, ConnectionState.INTERRUPTED);
                    if (++this.missedHeartBeats > HeartBeatMonitor.MAX_MISSED_HEARTBEATS) {
                        HeartBeatMonitor.log.warn("[{}] - Missed more than {} heartbeats, closing socket",
                                this.connectionId,
                                HeartBeatMonitor.MAX_MISSED_HEARTBEATS);
                        this.close();
                    }
                }

                try {
                    this.receivedPong = false;
                    // HeartBeatMonitor.log.trace("[{}] - PING ->", this.connectionId);
                    this.socket.send(HeartBeatMonitor.PING);
                } catch (final IOException e) {
                    HeartBeatMonitor.log.warn("[{}] - Unable to send heartbeat, closing socket", this.connectionId);
                    this.close();
                }
            } catch (final Exception e) {
                HeartBeatMonitor.log.error("[{}] - Error while sending heartbeat", this.connectionId, e);
                this.close();
            }
        },
                HeartBeatMonitor.HEARTBEAT_INITIAL_DELAY,
                HeartBeatMonitor.HEARTBEAT_PERIOD_IN_SECONDS,
                HeartBeatMonitor.HEARTBEAT_TIMING_UNIT);

    }

    @Override
    public void close() {
        this.stop();
        this.socket.close();
    }

    /**
     * Stops the monitor from sending bytes or responding in the future. This would be desireable for example when the
     * connection is suspended.
     */
    public void stop() {
        if (this.heartBeatFuture != null) {
            this.heartBeatFuture.cancel(true);
            this.heartBeatFuture = null;
        }

        this.executor.shutdown();
    }

}
