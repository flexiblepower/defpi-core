/**
 * File TCPSocket.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.commons;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ClosedChannelException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TCPSocket
 *
 * @author coenvl
 * @version 0.1
 * @since Oct 11, 2017
 */
public class TCPSocket implements Closeable {

    private static final Collection<TCPSocket> ALL_SOCKETS = new LinkedList<>();

    private static final int EOM = 0xFF;
    protected static final Logger log = LoggerFactory.getLogger(TCPSocket.class);

    private static int threadCounter = 0;

    private final ExecutorService executor = Executors
            .newSingleThreadScheduledExecutor(r -> new Thread(r, "TCPthread " + TCPSocket.threadCounter++));
    // private final Thread connectionThread;
    private final Object waitLock = new Object();

    protected Socket clientSocket;
    protected ServerSocket serverSocket;
    protected InputStream inputStream;
    protected OutputStream outputStream;
    protected boolean ready = false;

    protected volatile boolean keepOpen = true;

    public static synchronized TCPSocket asClient(final String targetAddress, final int port) {
        return new TCPSocket(targetAddress, port);
    }

    public static synchronized TCPSocket asServer(final int port) {
        return new TCPSocket(port);
    }

    public static void destroyLingeringSockets() {
        TCPSocket.ALL_SOCKETS.forEach(s -> s.close());
        TCPSocket.ALL_SOCKETS.clear();
    }

    private TCPSocket(final int port) {
        this.executor.submit(new ServerSocketRunner(port));
        TCPSocket.ALL_SOCKETS.add(this);
    }

    private TCPSocket(final String address, final int port) {
        this.executor.submit(new ClientSocketRunner(address, port));
        TCPSocket.ALL_SOCKETS.add(this);
    }

    public boolean ready() {
        return this.ready;
    }

    public boolean isConnected() {
        return (this.clientSocket != null) && this.clientSocket.isConnected() && !this.clientSocket.isClosed();
    }

    public boolean isClosed() {
        return (this.clientSocket != null) && this.clientSocket.isClosed();
    }

    public void waitUntilConnected(final long millis) throws InterruptedException, IOException {
        if (this.ready()) {
            return;
        }

        synchronized (this.waitLock) {
            if (millis < 1) {
                this.waitLock.wait();
            } else {
                this.waitLock.wait(millis);
            }
        }

        if (!this.isConnected()) {
            throw new ClosedChannelException();
        }
    }

    protected void releaseWaitLock() {
        synchronized (this.waitLock) {
            this.waitLock.notifyAll();
        }
    }

    public byte[] read(final int timeout) throws InterruptedException, ExecutionException, TimeoutException {
        return this.executor.submit(() -> this.read()).get(timeout, TimeUnit.MILLISECONDS);
    }

    public byte[] read() throws InterruptedException, IOException {
        // TCPSocket.log.trace("Waiting to read...");
        if (this.isClosed()) {
            throw new ClosedChannelException();
        }
        // this.waitUntilConnected(0);
        synchronized (this.inputStream) {
            // TCPSocket.log.trace("Allowed to read");
            final int len = (this.inputStream.read() * 256) + this.inputStream.read();
            if (len < 0) {
                throw new IOException("Reached end of stream");
            }
            // TCPSocket.log.trace("Reading {} bytes", len);
            final byte[] data = new byte[len];
            int read = 0;
            while (read < len) {
                read += this.inputStream.read(data, read, len - read);
            }
            if (read != len) {
                TCPSocket.log.warn("Expected {} bytes, instead received {}", len, read);
            }
            int eof = this.inputStream.read();
            if (eof != TCPSocket.EOM) {
                TCPSocket.log.warn("Expected EOM, instead read {}, skipping stream", eof);
                while (eof != TCPSocket.EOM) {
                    eof = this.inputStream.read();
                }
            }
            // TCPSocket.log.trace("Finished read: {}", new String(data).replace("\0", "\\0"));
            return data;
        }
    }

    public void send(final byte[] data, final int timeout) throws InterruptedException,
            ExecutionException,
            TimeoutException {
        this.executor.submit(() -> {
            this.send(data);
            return true;
        }).get(timeout, TimeUnit.MILLISECONDS);
    }

    public void send(final byte[] data) throws InterruptedException, IOException {
        // TCPSocket.log.trace("Waiting to send...");
        if (this.isClosed()) {
            throw new ClosedChannelException();
        }
        // this.waitUntilConnected(0);
        synchronized (this.outputStream) {
            // TCPSocket.log.trace("Sending {} bytes", data.length);
            this.outputStream.write(data.length / 256);
            this.outputStream.write(data.length % 256);
            this.outputStream.write(data);
            this.outputStream.write(TCPSocket.EOM);
            this.outputStream.flush();
        }
    }

    @Override
    public synchronized void close() {
        this.keepOpen = false;

        if (this.serverSocket != null) {
            try {
                this.serverSocket.close();
            } catch (final IOException e) {
                TCPSocket.log.warn("Exception while closing socket: {}", e.getMessage());
            }
            this.serverSocket = null;
        }

        if (this.clientSocket != null) {
            try {
                this.clientSocket.close();
            } catch (final IOException e) {
                TCPSocket.log.warn("Exception while closing socket: {}", e.getMessage());
            }
            this.clientSocket = null;
        }

        this.executor.shutdownNow();
        this.releaseWaitLock();
    }

    /**
     * SocketRunner makes sure the socket intialized
     *
     * @author coenvl
     * @version 0.1
     * @since Oct 11, 2017
     */
    protected abstract class SocketRunner implements Runnable {

        private static final long INITIAL_BACKOFF_MS = 50;
        private static final long MAXIMUM_BACKOFF_MS = 60000;
        private long backOffMs;

        abstract protected Socket init();

        @Override
        public void run() {
            this.backOffMs = SocketRunner.INITIAL_BACKOFF_MS;
            try {
                TCPSocket.this.clientSocket = this.init();

                if (!TCPSocket.this.keepOpen) {
                    // Check if in the mean time we got a close request...
                    TCPSocket.this.close();
                    return;
                }

                TCPSocket.this.inputStream = TCPSocket.this.clientSocket.getInputStream();
                TCPSocket.this.outputStream = TCPSocket.this.clientSocket.getOutputStream();
                TCPSocket.this.ready = true;
                TCPSocket.this.releaseWaitLock();
            } catch (final IOException e) {
                TCPSocket.log.warn("Exception while initializing socket: {}", e.getMessage());
            }
        }

        protected void increaseBackOffAndWait() {
            this.backOffMs = (long) Math.min(SocketRunner.MAXIMUM_BACKOFF_MS, this.backOffMs * 1.25);
            try {
                Thread.sleep(this.backOffMs);
            } catch (final InterruptedException e) {
                // Don't care, we'll see you next iteration
            }
        }
    }

    private final class ClientSocketRunner extends SocketRunner {

        private final String targetAddress;
        private final int targetPort;

        /**
         * @param address
         * @param port
         */
        public ClientSocketRunner(final String address, final int port) {
            this.targetAddress = address;
            this.targetPort = port;
        }

        @Override
        protected Socket init() {
            while (TCPSocket.this.keepOpen) {
                try {
                    final Socket client = new Socket(this.targetAddress, this.targetPort);
                    // client.setTcpNoDelay(false);
                    // client.setReuseAddress(false);
                    // client.setOOBInline(false);
                    // client.setSoLinger(false, 0);
                    // client.setPerformancePreferences(0, 1, 1);
                    TCPSocket.log.info("Initialized client socket to {}", client.getRemoteSocketAddress());
                    return client;
                } catch (final IOException e) {
                    TCPSocket.log.trace("Unable to connect ({}), retrying...", e.getMessage());
                    this.increaseBackOffAndWait();
                }
            }
            return null;
        }

    }

    private final class ServerSocketRunner extends SocketRunner {

        private final int listenPort;

        /**
         * @param port
         */
        public ServerSocketRunner(final int port) {
            this.listenPort = port;
        }

        @Override
        protected Socket init() {
            while (TCPSocket.this.keepOpen) {
                try {
                    TCPSocket.log.info("Starting server socket at {}", this.listenPort);
                    TCPSocket.this.serverSocket = new ServerSocket(this.listenPort);
                    // TCPSocket.this.serverSocket.setReuseAddress(false);
                    // TCPSocket.this.serverSocket.setPerformancePreferences(0, 1, 1);
                    final Socket client = TCPSocket.this.serverSocket.accept();
                    TCPSocket.log.info("Accepted client socket at {}", client.getRemoteSocketAddress());
                    return client;
                } catch (final IOException e) {
                    TCPSocket.log.trace("Unable to listen ({}), retrying...", e.getMessage());
                    if (TCPSocket.this.serverSocket != null) {
                        try {
                            TCPSocket.this.serverSocket.close();
                        } catch (final IOException e2) {
                            // It's okay... just try again in a bit
                        }
                        TCPSocket.this.serverSocket = null;
                    }
                    this.increaseBackOffAndWait();
                }
            }
            return null;
        }

    }

}