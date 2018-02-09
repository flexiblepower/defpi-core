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
 * The TCPSocket is a wrapper class around the {@linkplain java.net.Socket} to make sure that byte arrays are received
 * as a whole. (i.e. some checks are added to make sure byte arrays are sent with an additional four bytes indicating
 * the length, and an END-OF-MESSAGE byte is added at the end.)
 * <p>
 * TCPSockets are meant as disposable, they will function as long as the socket is alive, but when an IOException occurs
 * that is non-recoverable (i.e a remote hangup, end-of-stream) the socket is closed. The only iterative attempts to
 * connect the socket occur when initiating the connection, for example when calling {@linkplain #waitUntilConnected()}.
 *
 * @author Coen van Leeuwen
 * @version 0.3
 * @since Oct 11, 2017
 */
public class TCPSocket implements Closeable {

    /**
     * Logger for all TCPSocket events
     */
    protected static final Logger log = LoggerFactory.getLogger(TCPSocket.class);

    private static final int CONNECT_ON_SEND_TIMEOUT = 10;
    private static final int EOM = 0xFF;

    private final SocketConnector connector;

    /**
     * The backing java.net.Socket that this class wraps around
     */
    protected Socket socket;

    /**
     * Builder function to create a new TCP socket as a client, connecting to a Server socket
     *
     * @param host the target host name of the server to connect to.
     * @param port the port of the remote server to connect to
     * @return A TCPSocket that will try to connect to the provided host and port
     * @see java.net.Socket#Socket(String, int)
     */
    public static synchronized TCPSocket asClient(final String host, final int port) {
        return new TCPSocket(host, port);
    }

    /**
     * Builder function to create a new TCP socket as a server, receiving a connection from a Client socket
     *
     * @param port the port of the socket to bind to
     * @return A TCPSocket that will try to bind to the provided port
     * @see java.net.ServerSocket#ServerSocket(int)
     */
    public static synchronized TCPSocket asServer(final int port) {
        return new TCPSocket(port);
    }

    private TCPSocket(final int port) {
        this.connector = new ServerSocketConnector(port);
    }

    private TCPSocket(final String address, final int port) {
        this.connector = new ClientSocketConnector(address, port);
    }

    /**
     * This function will check if the socket is connected or not. Contrary to
     * {@linkplain java.net.Socket#isConnected()}, this function will return when the socket is closed.
     *
     * @return a boolean indicating whether the TCPSocket is connected or not.
     */
    public boolean isConnected() {
        return (this.socket != null) && this.socket.isConnected() && !this.socket.isClosed();
    }

    /**
     * This function will return if the socket has been closed. This is true after {@linkplain #close()} has been
     * called, or
     * when an IOException has occurred during a read or send operation.
     *
     * @return a boolean indicating whether the TCPSocket has been closed.
     */
    public boolean isClosed() {
        return (this.socket != null) && this.socket.isClosed();
    }

    /**
     * Calling this function will block forever until either an IOException occurred, or the socket successfully
     * connected. Common exceptions that occur in the underlying Socket are ignored, and a retry is scheduled until it
     * succeeds, or a non-recoverable exception occurs.
     *
     * @throws IOException when the underlying socket throws an exception while waiting to connect
     * @see #waitUntilConnected(long)
     */
    public synchronized void waitUntilConnected() throws IOException {
        this.waitUntilConnected(0);
    }

    /**
     * Calling this function will until either an IOException occurred, the socket successfully
     * connected, or the timeout has passed. Common exceptions that occur in the underlying Socket are ignored, and a
     * retry is scheduled until it succeeds, or a non-recoverable exception occurs.
     *
     * @param millis the amount of milliseconds to wait. When 0 entered this function behaves identical as
     *            {@linkplain #waitUntilConnected()}
     * @return whether the socket connected
     * @throws IOException when the underlying socket throws an exception while waiting to connect
     */
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
            this.socket.setKeepAlive(true);
            return true;
        }
    }

    /**
     * Try to read data from the socket, blocking forever until the data is read, or an exception occurs.
     * <p>
     * It is not necessary to use {@linkplain #waitUntilConnected()} before calling this function; if the socket was not
     * connected before using {@linkplain #read()}, it will be connected before attempting to read from the socket.
     *
     * @return the data that was read from the socket
     * @throws ClosedChannelException when this socket has been closed
     * @throws IOException When the underlying socket is closed before the data is received, the end-of-stream is
     *             reached, or any exception occurs while reading, or while waiting for the data
     * @see #read(long)
     */
    public byte[] read() throws IOException {
        return this.read(0);
    }

    /**
     * Try to read data from the socket, blocking until the data is read, the maximum time has passed, or an exception
     * occurs. If a timeout of 0 is provided, this function behaves identical to {@linkplain #read()}.
     * <p>
     * It is not necessary to use {@linkplain #waitUntilConnected(long)} before calling this function; if the socket was
     * not
     * connected before using {@linkplain #read(long)}, it will be connected before attempting to read from the socket.
     * The
     * time spent waiting to connect will be subtracted from the timeout.
     *
     * @param timeout the amount of milliseconds to wait, before returning null
     * @return the data that was read from the socket or null if no data was read.
     * @throws ClosedChannelException when this socket has been closed
     * @throws IOException When the underlying socket is closed before the data is received, the end-of-stream is
     *             reached, or any exception occurs while reading, or while waiting for the data.
     */
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
                TCPSocket.log.trace("Read timeout while waiting to connect");
                return null;
            }
        }

        synchronized (this.socket.getInputStream()) {
            if (timeout == 0) {
                this.socket.setSoTimeout(0);
            } else {
                final int newTimeout = (int) Math.max(1, timeout - (System.currentTimeMillis() - t_start));
                this.socket.setSoTimeout(newTimeout);
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
                TCPSocket.log.trace("Read timeout while waiting for data");
                return null;
            }
        }
    }

    /**
     * Try to send data to the socket.
     * <p>
     * It is not necessary to use {@linkplain #waitUntilConnected()} before calling this function; but it is
     * recommended.
     * If the socket was not connected before using {@linkplain #send(byte[])}, it will be attempted to connect, but
     * with a
     * very brief timeout, which will only succeed if the remote socket is already attempting to connect.
     *
     * @param data The byte array to send through the socket
     * @throws NotYetConnectedException when the socket is not yet connected, and fails to connect immediately
     * @throws ClosedChannelException when this socket has been closed
     * @throws IOException When the underlying socket is closed before the data is sent, or while waiting for the data
     */
    public void send(final byte[] data) throws IOException {
        if (!this.waitUntilConnected(TCPSocket.CONNECT_ON_SEND_TIMEOUT)) {
            throw new NotYetConnectedException();
        } else if (this.isClosed()) {
            throw new ClosedChannelException();
        }

        synchronized (this.socket.getOutputStream()) {
            try {
                // We should not close this stream, as it will also close the socket
                @SuppressWarnings("resource")
                final OutputStream os = this.socket.getOutputStream();
                os.write(ByteBuffer.allocate(4).putInt(data.length).array());
                os.write(data);
                os.write(TCPSocket.EOM);
                os.flush();
            } catch (final IOException e) {
                this.close();
                throw e;
            }
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
     * SocketConnector makes sure the socket initializes
     *
     * @version 0.1
     * @since Oct 11, 2017
     */
    protected abstract class SocketConnector implements Closeable {

        private static final long INITIAL_BACKOFF_MS = 50;
        private static final long MAXIMUM_BACKOFF_MS = 60000;
        private long backOffMs = SocketConnector.INITIAL_BACKOFF_MS;

        /**
         * Set up the connection
         *
         * @param millis the timeout in milliseconds
         * @return the Socket that is the result of a successfull connection, or null if the timeout has passed before
         *         the connection succeeded
         * @throws IOException
         */
        public abstract Socket connect(final long millis) throws IOException;

        /**
         * Increase the timeout and wait before re-attempting the connection
         *
         * @param max a maximum timeout to respect
         */
        protected void increaseBackOffAndWait(final long max) {
            this.backOffMs = (long) Math.min(max, Math.min(SocketConnector.MAXIMUM_BACKOFF_MS, this.backOffMs * 1.25));
            try {
                Thread.sleep(this.backOffMs);
            } catch (final InterruptedException e) {
                // Don't care, we'll see you next iteration
            }
        }

        /**
         * Compute the time left when a call has started at some timestamp and there is a certain amount of maxmimum
         * milliseconds to wait
         *
         * @param t_start the time in milliseconds as returned by {@linkplain java.lang.System#currentTimeMillis()}
         * @param millis the amount of milliseconds to wait at most
         * @return the maximum backoff period when the timeout is zero, or the amount of remaining milliseconds in the
         *         timeout
         */
        protected long timeLeft(final long t_start, final long millis) {
            return millis == 0 ? SocketConnector.MAXIMUM_BACKOFF_MS
                    : Math.max(0, millis - (System.currentTimeMillis() - t_start));
        }

    }

    private final class ClientSocketConnector extends SocketConnector {

        private final String targetAddress;
        private final int targetPort;

        /**
         * Creates a SocketConnector that will initiate a client socket, connecting to the provided host name and port
         *
         * @param address the host address to connect to
         * @param port the remote port to connect to
         * @see java.net.Socket#Socket(String, int)
         */
        public ClientSocketConnector(final String address, final int port) {
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

    private final class ServerSocketConnector extends SocketConnector {

        private final int serverPort;
        private ServerSocket serverSocket;

        /**
         * Creates a SocketConnector that will initiate a server socket, binding to the provided port
         *
         * @param port the local port to bind to
         * @see java.net.ServerSocket#ServerSocket(int)
         */
        public ServerSocketConnector(final int port) {
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

            this.serverSocket.setSoTimeout((int) Math.max(1, this.timeLeft(t_start, millis)));
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
