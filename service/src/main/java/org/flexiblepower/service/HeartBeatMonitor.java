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

    private static int threadCount = 0;

    private final ScheduledExecutorService executor;
    private final ManagedConnection connection;

    private ScheduledFuture<?> heartBeatFuture;
    private boolean receivedPong;

    HeartBeatMonitor(final ManagedConnection c) {
        this.connection = c;
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
            this.receivedPong = true;
            return true;
        } else if (Arrays.equals(data, HeartBeatMonitor.PING) && this.connection.isConnected()) {
            // If pinged, respond with a pong
            HeartBeatMonitor.log.trace("PONG");
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
        this.heartBeatFuture = this.executor.scheduleAtFixedRate(() -> {
            try {
                if (!this.connection.isConnected()) {
                    return;
                }

                if (!this.receivedPong) {
                    // If no PONG was received since the last PING, assume connection was interrupted!
                    HeartBeatMonitor.log.warn("No heartbeat received on connection, goto {}",
                            ConnectionState.INTERRUPTED);
                    this.connection.goToInterruptedState();
                }

                HeartBeatMonitor.log.trace("PING");
                if (this.connection.sendRaw(HeartBeatMonitor.PING)) {
                    this.receivedPong = false;
                } else {
                    HeartBeatMonitor.log.warn("Unable to send heartbeat, goto {}", ConnectionState.INTERRUPTED);
                    this.connection.goToInterruptedState();
                }

            } catch (final Exception e) {
                HeartBeatMonitor.log.error("Error while sending heardbeat", e);
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
