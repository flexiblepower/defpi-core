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
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TCPSocket
 *
 * @version 0.1
 * @since Oct 11, 2017
 */
public class TCPSocket implements Closeable {

    private static final int CONNECT_ON_SEND_TIMEOUT = 10;
    private static final int EOM = 0xFF;
    protected static final Logger log = LoggerFactory.getLogger(TCPSocket.class);

    private final SocketRunner connector;

    protected Socket socket;

    public static synchronized TCPSocket asClient(final String targetAddress, final int port) {
        return new TCPSocket(targetAddress, port);
    }

    public static synchronized TCPSocket asServer(final int port) {
        return new TCPSocket(port);
    }

    private TCPSocket(final int port) {
        this.connector = new ServerSocketRunner(port);
    }

    private TCPSocket(final String address, final int port) {
        this.connector = new ClientSocketRunner(address, port);
    }

    public boolean isConnected() {
        return (this.socket != null) && this.socket.isConnected() && !this.socket.isClosed();
    }

    public boolean isClosed() {
        return (this.socket != null) && this.socket.isClosed();
    }

    public synchronized void waitUntilConnected() throws IOException {
        this.waitUntilConnected(0);
    }

    public synchronized boolean waitUntilConnected(final long millis) throws IOException {
        if (this.socket != null) {
            return true;
        }

        this.socket = this.connector.connect(millis);
        if (this.socket == null) {
            return false;
        } else {
            // So we DID get a socket, but it somehow poofed away
            if (!this.isConnected()) {
                throw new ClosedChannelException();
            }
            return true;
        }
    }

    public byte[] read() throws IOException {
        return this.read(0);
    }

    @SuppressWarnings("resource")
    public byte[] read(final long timeout) throws IOException {
        if (this.isClosed()) {
            throw new ClosedChannelException();
        }

        final long t_start = System.currentTimeMillis();
        if (timeout == 0) {
            this.waitUntilConnected();
        } else {
            if (!this.waitUntilConnected(timeout)) {
                return null;
            }
        }

        synchronized (this.socket.getInputStream()) {
            if (timeout == 0) {
                this.socket.setSoTimeout(0);
            } else {
                this.socket.setSoTimeout((int) (timeout - (System.currentTimeMillis() - t_start)));
            }

            // Read 4 bytes that will tell how long the message is
            try {
                final InputStream is = this.socket.getInputStream();
                final int len = ByteBuffer
                        .wrap(new byte[] {(byte) is.read(), (byte) is.read(), (byte) is.read(), (byte) is.read()})
                        .getInt();

                if (len < 0) {
                    this.close();
                    throw new IOException("Reached end of stream");
                }
                final byte[] data = new byte[len];
                int read = 0;
                while (read < len) {
                    read += is.read(data, read, len - read);
                }
                if (read != len) {
                    TCPSocket.log.warn("Expected {} bytes, instead received {}", len, read);
                }

                int eof = is.read();
                if (eof != TCPSocket.EOM) {
                    TCPSocket.log.warn("Expected EOM, instead read {}, skipping stream", eof);
                    while (eof != TCPSocket.EOM) {
                        eof = is.read();
                        if (eof < 0) {
                            this.close();
                            throw new IOException("Reached end of stream");
                        }
                    }
                }

                return data;
            } catch (final SocketTimeoutException e) {
                return null;
            }
        }
    }

    public void send(final byte[] data) throws IOException {
        if (!this.waitUntilConnected(TCPSocket.CONNECT_ON_SEND_TIMEOUT)) {
            throw new NotYetConnectedException();
        } else if (this.isClosed()) {
            throw new ClosedChannelException();
        }

        synchronized (this.socket.getOutputStream()) {
            final OutputStream os = this.socket.getOutputStream();
            os.write(ByteBuffer.allocate(4).putInt(data.length).array());
            os.write(data);
            os.write(TCPSocket.EOM);
            os.flush();
        }
    }

    @Override
    public void close() {
        try {
            this.connector.close();
        } catch (final IOException e) {
            TCPSocket.log.warn("Exception while closing connector: {}", e.getMessage());
        }

        if ((this.socket != null) && !this.socket.isClosed()) {
            try {
                this.socket.close();
            } catch (final IOException e) {
                TCPSocket.log.warn("Exception while closing socket: {}", e.getMessage());
            }
        }
    }

    /**
     * SocketRunner makes sure the socket intialized
     *
     * @version 0.1
     * @since Oct 11, 2017
     */
    protected abstract class SocketRunner implements Closeable {

        private static final long INITIAL_BACKOFF_MS = 50;
        private static final long MAXIMUM_BACKOFF_MS = 60000;
        private long backOffMs = SocketRunner.INITIAL_BACKOFF_MS;

        public abstract Socket connect(long millis) throws IOException;

        protected void increaseBackOffAndWait(final long max) {
            this.backOffMs = (long) Math.min(max, Math.min(SocketRunner.MAXIMUM_BACKOFF_MS, this.backOffMs * 1.25));
            try {
                Thread.sleep(this.backOffMs);
            } catch (final InterruptedException e) {
                // Don't care, we'll see you next iteration
            }
        }

        protected long timeLeft(final long t_start, final long millis) {
            return millis == 0 ? SocketRunner.MAXIMUM_BACKOFF_MS
                    : Math.max(0, millis - (System.currentTimeMillis() - t_start));
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
        public Socket connect(final long millis) {
            final long t_start = System.currentTimeMillis();
            while (this.timeLeft(t_start, millis) > 0) {
                try {
                    final Socket client = new Socket(this.targetAddress, this.targetPort);
                    TCPSocket.log.info("Initialized client socket to {}", client.getRemoteSocketAddress());
                    return client;
                } catch (final IOException e) {
                    TCPSocket.log.trace("Unable to connect ({}), retrying...", e.getMessage());
                    this.increaseBackOffAndWait(this.timeLeft(t_start, millis));
                }
            }
            return null;
        }

        @Override
        public void close() throws IOException {
            // Do nothing
        }

    }

    private final class ServerSocketRunner extends SocketRunner {

        private final int serverPort;
        private ServerSocket serverSocket;

        /**
         * @param port
         */
        public ServerSocketRunner(final int port) {
            TCPSocket.log.info("Starting server socket at {}", port);
            this.serverPort = port;
            this.bindServerSocket();
        }

        private synchronized boolean bindServerSocket() {
            if (this.serverSocket != null) {
                return true;
            }

            try {
                TCPSocket.log.trace("Binding to port {}", this.serverPort);
                this.serverSocket = new ServerSocket(this.serverPort);
                this.serverSocket.setReuseAddress(true);
                return true;
            } catch (final IOException e) {
                TCPSocket.log.warn("Unable to open server socket at port {}: {}", this.serverPort, e.getMessage());
                return false;
            }
        }

        @Override
        public Socket connect(final long millis) throws IOException {
            final long t_start = System.currentTimeMillis();
            while ((this.serverSocket == null) && (this.timeLeft(t_start, millis) > 0)) {
                if (!this.bindServerSocket()) {
                    this.increaseBackOffAndWait(this.timeLeft(t_start, millis));
                }
            }

            if (this.serverSocket == null) {
                TCPSocket.log.trace("Server bind timed out");
                return null;
            }

            this.serverSocket.setSoTimeout((int) millis);
            try {
                final Socket client = this.serverSocket.accept();
                TCPSocket.log.info("Accepted client socket at {}", client.getRemoteSocketAddress());
                this.serverSocket.close();
                return client;
            } catch (final SocketTimeoutException e) {
                TCPSocket.log.trace("Server accept timed out");
                return null;
            } catch (final IOException e) {
                this.serverSocket.close();
                throw e;
            }
        }

        @Override
        public void close() throws IOException {
            if (this.serverSocket != null) {
                this.serverSocket.close();
            }
        }
    }

}
