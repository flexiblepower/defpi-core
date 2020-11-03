/*-
 * #%L
 * dEF-Pi commons Library
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

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.util.concurrent.TimeUnit;

import org.flexiblepower.commons.TCPSocket;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;

/**
 * ConnectionTest
 *
 * @version 0.1
 * @since May 12, 2017
 */
@SuppressWarnings({"static-method", "javadoc"})
@Timeout(value = 5, unit = TimeUnit.SECONDS)
public class TCPSocketTest {

    private static final int TEST_PORT = 5001;

    @RepeatedTest(3)
    public void doTest() throws Exception {
        try (
                final TCPSocket client = TCPSocket.asClient("127.0.0.1", TCPSocketTest.TEST_PORT);
                final TCPSocket server = TCPSocket.asServer(TCPSocketTest.TEST_PORT)) {
            client.waitUntilConnected(100);
            Assertions.assertTrue(client.isConnected());
            client.send("Test data".getBytes());
            Assertions.assertEquals("Test data", new String(server.read(100)));
            Assertions.assertTrue(server.isConnected());
        }
    }

    @RepeatedTest(3)
    public void doClose() throws Exception {
        try (
                final TCPSocket client = TCPSocket.asClient("127.0.0.1", TCPSocketTest.TEST_PORT);
                final TCPSocket server = TCPSocket.asServer(TCPSocketTest.TEST_PORT)) {
            client.waitUntilConnected(100);
            Assertions.assertTrue(client.isConnected());
            client.send("Test data".getBytes());
            Assertions.assertEquals("Test data", new String(server.read(100)));
            Assertions.assertTrue(server.isConnected());

            client.close();
            try {
                server.read(100);
            } catch (final Exception e) {
                Assertions.assertEquals(IOException.class, e.getClass());
            }

        }
    }

    @RepeatedTest(3)
    public void testTimeout() throws Exception {
        try (
                final TCPSocket client = TCPSocket.asClient("127.0.0.1", TCPSocketTest.TEST_PORT);
                final TCPSocket server = TCPSocket.asServer(TCPSocketTest.TEST_PORT)) {
            final int margin = 25;
            // Test 100 ms
            long t_start = System.currentTimeMillis();
            Assertions.assertNull(client.read(100));
            long t_wait = System.currentTimeMillis() - t_start;
            Assertions.assertTrue(Math.abs(t_wait - 100) < margin,
                    String.format("Timeout was %d (expected %d)", t_wait, 100));
            Assertions.assertTrue(client.isConnected());

            // Test 100 ms
            t_start = System.currentTimeMillis();
            Assertions.assertNull(server.read(100));
            t_wait = System.currentTimeMillis() - t_start;
            Assertions.assertTrue(Math.abs(t_wait - 100) < margin,
                    String.format("Timeout was %d (expected %d)", t_wait, 100));
            Assertions.assertTrue(server.isConnected(), "The server should be connected now");

            // Test 200 ms
            t_start = System.currentTimeMillis();
            Assertions.assertNull(server.read(200));
            t_wait = System.currentTimeMillis() - t_start;
            Assertions.assertTrue(Math.abs(t_wait - 200) < margin,
                    String.format("Timeout was %d (expected %d)", t_wait, 200));

            // Test if data still comes through
            client.send("Test data".getBytes());
            Assertions.assertEquals("Test data", new String(server.read()));
        }
    }

    @RepeatedTest(3)
    public void multiBindTest() throws Exception {
        final int connectTimeout = 100;

        // Setup the first server / client pair, which should do fine
        try (
                final TCPSocket server = TCPSocket.asServer(TCPSocketTest.TEST_PORT);
                final TCPSocket client = TCPSocket.asClient("127.0.0.1", TCPSocketTest.TEST_PORT);
                final TCPSocket server2 = TCPSocket.asServer(TCPSocketTest.TEST_PORT);
                final TCPSocket client2 = TCPSocket.asClient("127.0.0.1", TCPSocketTest.TEST_PORT)) {
            Assertions.assertFalse(server.isConnected());
            client.waitUntilConnected();
            client.send("randomBytes".getBytes());
            Assertions.assertTrue(client.isConnected());
            Assertions.assertNotNull(server.read());
            Assertions.assertTrue(server.isConnected());

            // Thread.sleep(100);

            // Setup the second server which should not work!
            Assertions.assertFalse(server2.isConnected());
            Assertions.assertNull(server2.read(connectTimeout));
            Assertions.assertFalse(server2.isConnected());
            Assertions.assertFalse(server2.waitUntilConnected(connectTimeout));
            Assertions.assertFalse(server2.isConnected());

            Assertions.assertFalse(client2.isConnected());
            Assertions.assertNull(client2.read(connectTimeout));
            Assertions.assertTrue(client2.isConnected());

            // Now close the first pair
            server.close();
            Assertions.assertTrue(server.isClosed());
            Assertions.assertFalse(server.isConnected());
            Assertions.assertTrue(client.isConnected(),
                    "The server is closed, but the client should only detect this when reading");
            Assertions.assertFalse(client.isClosed());

            try {
                client.read();
                Assertions.fail("Expected a " + IOException.class);
            } catch (final Exception e) {
                Assertions.assertEquals(IOException.class, e.getClass());
                Assertions.assertEquals("Reached end of stream", e.getMessage());
            }

            Assertions.assertFalse(client.isConnected());
            Assertions.assertTrue(client.isClosed());

            // Now the second pair should start to work
            Assertions.assertFalse(server2.isConnected());
            Assertions.assertNull(server2.read(connectTimeout));
            Assertions.assertTrue(server2.isConnected());

            client2.send("randomBytes".getBytes());
            Assertions.assertNotNull(server2.read());
            Assertions.assertTrue(client2.isConnected());
            Assertions.assertTrue(server2.isConnected());

            client2.close();
            server2.close();
        }
    }

    @RepeatedTest(3)
    public void multiConnectTest() throws Exception {

        // The thread will be always on!
        @SuppressWarnings("resource")
        final Thread serverThread = new Thread(() -> {
            try {
                TCPSocket server = TCPSocket.asServer(TCPSocketTest.TEST_PORT);
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        server.read(100);
                    } catch (final Exception e) {
                        // Reached end of stream...
                        server.close();
                        server = TCPSocket.asServer(TCPSocketTest.TEST_PORT);
                    }
                }
                server.close();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        });

        try (
                final TCPSocket clientSocket = TCPSocket.asClient("127.0.0.1", TCPSocketTest.TEST_PORT);
                final TCPSocket clientSocket2 = TCPSocket.asClient("127.0.0.1", TCPSocketTest.TEST_PORT)) {
            // the client socket won't be able to connect THIS fast
            clientSocket.waitUntilConnected(1);
            Assertions.assertFalse(clientSocket.isConnected(), "Client should not be able to open yet");
            Assertions.assertFalse(clientSocket.isClosed(), "Client should not closed yet");

            serverThread.start();

            // But given some more time it will....
            clientSocket.waitUntilConnected();
            Assertions.assertNull(clientSocket.read(100), "Client should not be able to read");
            Assertions.assertTrue(clientSocket.isConnected(), "Client should be connected by now");
            Assertions.assertFalse(clientSocket.isClosed(), "Client should not closed yet");

            // Send some data until we close or interrupt it
            final Thread client1 = new Thread(() -> {
                try {
                    while (true) {
                        clientSocket.send("This is a test".getBytes());
                        Thread.sleep(100);
                    }
                } catch (final Exception e) {
                    clientSocket.close();
                }
            });
            client1.start();

            clientSocket2.waitUntilConnected(200); // It won't be able to connect since the socket is "taken"
            Assertions.assertFalse(clientSocket2.isConnected(),
                    "Client should not be able to connect while it is taken");
            Assertions.assertFalse(clientSocket2.isClosed(), "Client should not closed yet");

            try {
                clientSocket2.send("Some stuff".getBytes());
                Assertions.fail();
            } catch (final Exception e) {
                Assertions.assertEquals(NotYetConnectedException.class, e.getClass());
            }

            final Thread client2 = new Thread(() -> {
                while (true) {
                    try {
                        clientSocket2.send("This is the second test".getBytes());
                        Thread.sleep(100);
                    } catch (final Exception e) {
                        clientSocket2.close();
                    }
                }
            });
            client2.start();

            if (Math.random() < 0.5) {
                client1.interrupt(); // Will make sure clientSocket1 is closed
            } else {
                clientSocket.close(); // Same effect
            }
            client1.join(10); // Wait until the thread completes
            Assertions.assertFalse(clientSocket.isConnected());
            Assertions.assertTrue(clientSocket.isClosed());

            try {
                clientSocket.send("Some stuff".getBytes());
                Assertions.fail();
            } catch (final Exception e) {
                Assertions.assertEquals(ClosedChannelException.class, e.getClass());
            }

            clientSocket2.waitUntilConnected();
            Assertions.assertTrue(clientSocket2.isConnected());
            client2.interrupt(); // Will make sure clientSocket2 is closed
            client2.join(100);
            serverThread.interrupt();
            serverThread.join(100); // Wait until the thread completes

            Assertions.assertFalse(clientSocket2.isConnected());
            Assertions.assertTrue(clientSocket2.isClosed());
        }
    }

}
