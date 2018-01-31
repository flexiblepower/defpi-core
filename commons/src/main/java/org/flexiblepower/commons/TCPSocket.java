/**
 * File TCPSocket.java
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
package org.flexiblepower.commons;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TCPSocket
 *
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
    private final Object waitLock = new Object();

    private final ExecutorService readExecutor = Executors.newSingleThreadExecutor();
    private Future<byte[]> readFuture;

    protected Socket socket;

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
        synchronized (TCPSocket.ALL_SOCKETS) {
            TCPSocket.ALL_SOCKETS.forEach(s -> s.close());
            TCPSocket.ALL_SOCKETS.clear();
        }
    }

    private TCPSocket(final int port) {
        this.executor.submit(new ServerSocketRunner(port));
        synchronized (TCPSocket.ALL_SOCKETS) {
            TCPSocket.ALL_SOCKETS.add(this);
        }
    }

    private TCPSocket(final String address, final int port) {
        this.executor.submit(new ClientSocketRunner(address, port));
        synchronized (TCPSocket.ALL_SOCKETS) {
            TCPSocket.ALL_SOCKETS.add(this);
        }
    }

    public boolean ready() {
        return this.ready;
    }

    public boolean isConnected() {
        return (this.socket != null) && this.socket.isConnected() && !this.socket.isClosed();
    }

    public boolean isClosed() {
        return (this.socket != null) && this.socket.isClosed();
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

    public byte[] read(final int timeout) throws IOException, InterruptedException {
        if (this.readFuture == null) {
            this.readFuture = this.readExecutor.submit(() -> this.doRead());
        }

        try {
            final byte[] ret = this.readFuture.get(timeout, TimeUnit.MILLISECONDS);
            this.readFuture = null;
            return ret;
        } catch (final TimeoutException e) {
            return null;
        } catch (final ExecutionException e) {
            this.readFuture = null;
            throw new IOException(e);
        }
    }

    public byte[] read() throws IOException {
        if (this.readFuture != null) {
            try {
                return this.readFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                this.readFuture = null;
                throw new IOException(e);
            }
        }

        return this.doRead();
    }

    private byte[] doRead() throws IOException {
        if (!this.ready()) {
            try {
                this.waitUntilConnected(0);
            } catch (final InterruptedException e) {
                throw new IOException(e);
            }
        }

        if (this.isClosed()) {
            throw new ClosedChannelException();
        }

        synchronized (this.inputStream) {
            // Read 4 bytes that will tell how long the message is
            final int len = ByteBuffer.wrap(new byte[] {(byte) this.inputStream.read(), (byte) this.inputStream.read(),
                    (byte) this.inputStream.read(), (byte) this.inputStream.read()}).getInt();
            if (len < 0) {
                throw new IOException("Reached end of stream");
            }
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
            return data;
        }
    }

    public void send(final byte[] data) throws IOException {
        if (!this.ready()) {
            try {
                this.waitUntilConnected(0);
            } catch (final InterruptedException e) {
                throw new IOException(e);
            }
        }

        if (this.isClosed()) {
            throw new ClosedChannelException();
        }
        synchronized (this.outputStream) {
            this.outputStream.write(ByteBuffer.allocate(4).putInt(data.length).array());
            this.outputStream.write(data);
            this.outputStream.write(TCPSocket.EOM);
            this.outputStream.flush();
        }
    }

    @Override
    public synchronized void close() {
        this.keepOpen = false;

        if (this.socket != null) {
            try {
                this.socket.close();
            } catch (final IOException e) {
                TCPSocket.log.warn("Exception while closing socket: {}", e.getMessage());
            }
            this.socket = null;
        }

        this.executor.shutdownNow();
        this.releaseWaitLock();
    }

    /**
     * SocketRunner makes sure the socket intialized
     *
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
                TCPSocket.this.socket = this.init();

                if (!TCPSocket.this.keepOpen) {
                    // Check if in the mean time we got a close request...
                    TCPSocket.this.close();
                    return;
                }

                TCPSocket.this.inputStream = TCPSocket.this.socket.getInputStream();
                TCPSocket.this.outputStream = TCPSocket.this.socket.getOutputStream();
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
                try (final ServerSocket serverSocket = new ServerSocket(this.listenPort)) {
                    TCPSocket.log.info("Starting server socket at {}", this.listenPort);
                    final Socket client = serverSocket.accept();
                    TCPSocket.log.info("Accepted client socket at {}", client.getRemoteSocketAddress());
                    return client;
                } catch (final IOException e) {
                    TCPSocket.log.trace("Unable to listen ({}), retrying...", e.getMessage());
                    this.increaseBackOffAndWait();
                }
            }
            return null;
        }

    }

}
