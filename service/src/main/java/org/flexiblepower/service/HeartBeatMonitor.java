/**
 * File HeartBeatMonitor.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.io.Closeable;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.flexiblepower.proto.ConnectionProto.ConnectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HeartBeatMonitor
 *
 * @author coenvl
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

    private final ManagedConnection connection;
    private final String connectionId;
    private final ScheduledExecutorService executor;

    private ScheduledFuture<?> heartBeatFuture;
    private boolean receivedPong;
    private int missedHeartBeats;

    HeartBeatMonitor(final ManagedConnection c, final String connectionId) {
        this.connection = c;
        this.connectionId = connectionId;
        final ThreadFactory threadFactory = r -> new Thread(r, "dEF-Pi hbMonThread-" + HeartBeatMonitor.threadCount++);
        this.executor = Executors.newSingleThreadScheduledExecutor(threadFactory);
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
            HeartBeatMonitor.log.trace("[{}] - -> PONG", this.connectionId);
            this.receivedPong = true;
            this.missedHeartBeats = 0;
            return true;
        } else if (Arrays.equals(data, HeartBeatMonitor.PING)) {
            // If pinged, respond with a pong
            HeartBeatMonitor.log.trace("[{}] - PING -> PONG", this.connectionId);
            this.connection.sendRaw(HeartBeatMonitor.PONG);
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
                if (!this.connection.isConnected()) {
                    return;
                }

                if (!this.receivedPong) {
                    // If no PONG was received since the last PING, assume connection was interrupted!
                    HeartBeatMonitor.log
                            .warn("[{}] - Missed a heartbeat...", this.connectionId, ConnectionState.INTERRUPTED);
                    if (++this.missedHeartBeats > HeartBeatMonitor.MAX_MISSED_HEARTBEATS) {
                        HeartBeatMonitor.log.warn("[{}] - Missed more than {} heartbeats, goto {}",
                                this.connectionId,
                                HeartBeatMonitor.MAX_MISSED_HEARTBEATS,
                                ConnectionState.INTERRUPTED);
                        this.connection.goToInterruptedState();
                    }
                }

                this.receivedPong = false;
                HeartBeatMonitor.log.trace("[{}] - PING ->", this.connectionId);
                if (!this.connection.sendRaw(HeartBeatMonitor.PING)) {
                    HeartBeatMonitor.log.warn("[{}] - Unable to send heartbeat, goto {}",
                            this.connectionId,
                            ConnectionState.INTERRUPTED);
                    this.connection.goToInterruptedState();
                }

            } catch (final Exception e) {
                HeartBeatMonitor.log.error("[{}] - Error while sending heartbeat", this.connectionId, e);
            }
        },
                HeartBeatMonitor.HEARTBEAT_INITIAL_DELAY,
                HeartBeatMonitor.HEARTBEAT_PERIOD_IN_SECONDS,
                HeartBeatMonitor.HEARTBEAT_TIMING_UNIT);

    }

    @Override
    public void close() {
        if (this.heartBeatFuture != null) {
            this.heartBeatFuture.cancel(true);
            this.heartBeatFuture = null;
        }

        this.executor.shutdown();
    }

}
