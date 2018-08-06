/**
 * File TCPSocketTest.java
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

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.util.Arrays;
import java.util.List;

import org.flexiblepower.commons.TCPSocket;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * ConnectionTest
 *
 * @version 0.1
 * @since May 12, 2017
 */
@RunWith(Parameterized.class)
@SuppressWarnings({"static-method", "javadoc"})
public class TCPSocketTest {

    private static final int TEST_PORT = 5001;

    @Parameters
    public static List<Object[]> data() {
        return Arrays.asList(new Object[3][0]);
    }

    @Rule
    public Timeout globalTimeout = Timeout.seconds(5);

    @Test
    public void doTest() throws Exception {
        try (
                final TCPSocket client = TCPSocket.asClient("127.0.0.1", TCPSocketTest.TEST_PORT);
                final TCPSocket server = TCPSocket.asServer(TCPSocketTest.TEST_PORT)) {
            client.waitUntilConnected(100);
            Assert.assertTrue(client.isConnected());
            client.send("Test data".getBytes());
            Assert.assertEquals("Test data", new String(server.read(100)));
            Assert.assertTrue(server.isConnected());
        }
    }

    @Test
    public void doClose() throws Exception {
        try (
                final TCPSocket client = TCPSocket.asClient("127.0.0.1", TCPSocketTest.TEST_PORT);
                final TCPSocket server = TCPSocket.asServer(TCPSocketTest.TEST_PORT)) {
            client.waitUntilConnected(100);
            Assert.assertTrue(client.isConnected());
            client.send("Test data".getBytes());
            Assert.assertEquals("Test data", new String(server.read(100)));
            Assert.assertTrue(server.isConnected());

            client.close();
            try {
                server.read(100);
            } catch (final Exception e) {
                Assert.assertEquals(IOException.class, e.getClass());
            }

        }
    }

    @Test
    public void testTimeout() throws Exception {
        try (
                final TCPSocket client = TCPSocket.asClient("127.0.0.1", TCPSocketTest.TEST_PORT);
                final TCPSocket server = TCPSocket.asServer(TCPSocketTest.TEST_PORT)) {
            final int margin = 25;
            // Test 100 ms
            long t_start = System.currentTimeMillis();
            Assert.assertNull(client.read(100));
            long t_wait = System.currentTimeMillis() - t_start;
            Assert.assertTrue(String.format("Timeout was %d (expected %d)", t_wait, 100),
                    Math.abs(t_wait - 100) < margin);
            Assert.assertTrue(client.isConnected());

            // Test 100 ms
            t_start = System.currentTimeMillis();
            Assert.assertNull(server.read(100));
            t_wait = System.currentTimeMillis() - t_start;
            Assert.assertTrue(String.format("Timeout was %d (expected %d)", t_wait, 100),
                    Math.abs(t_wait - 100) < margin);
            Assert.assertTrue("The server should be connected now", server.isConnected());

            // Test 200 ms
            t_start = System.currentTimeMillis();
            Assert.assertNull(server.read(200));
            t_wait = System.currentTimeMillis() - t_start;
            Assert.assertTrue(String.format("Timeout was %d (expected %d)", t_wait, 200),
                    Math.abs(t_wait - 200) < margin);

            // Test if data still comes through
            client.send("Test data".getBytes());
            Assert.assertEquals("Test data", new String(server.read()));
        }
    }

    @Test
    public void multiBindTest() throws Exception {
        final int connectTimeout = 100;

        // Setup the first server / client pair, which should do fine
        try (
                final TCPSocket server = TCPSocket.asServer(TCPSocketTest.TEST_PORT);
                final TCPSocket client = TCPSocket.asClient("127.0.0.1", TCPSocketTest.TEST_PORT);
                final TCPSocket server2 = TCPSocket.asServer(TCPSocketTest.TEST_PORT);
                final TCPSocket client2 = TCPSocket.asClient("127.0.0.1", TCPSocketTest.TEST_PORT)) {
            Assert.assertFalse(server.isConnected());
            client.waitUntilConnected();
            client.send("randomBytes".getBytes());
            Assert.assertTrue(client.isConnected());
            Assert.assertNotNull(server.read());
            Assert.assertTrue(server.isConnected());

            Thread.sleep(100);

            // Setup the second server which should not work!
            Assert.assertFalse(server2.isConnected());
            Assert.assertNull(server2.read(connectTimeout));
            Assert.assertFalse(server2.isConnected());
            Assert.assertFalse(server2.waitUntilConnected(connectTimeout));
            Assert.assertFalse(server2.isConnected());

            Assert.assertFalse(client2.isConnected());
            Assert.assertNull(client2.read(connectTimeout));
            Assert.assertTrue(client2.isConnected());

            // Now close the first pair
            server.close();
            Assert.assertTrue(server.isClosed());
            Assert.assertFalse(server.isConnected());
            Assert.assertTrue("The server is closed, but the client should only detect this when reading",
                    client.isConnected());
            Assert.assertFalse(client.isClosed());

            try {
                client.read();
                Assert.fail("Expected a " + IOException.class);
            } catch (final Exception e) {
                Assert.assertEquals(IOException.class, e.getClass());
                Assert.assertEquals("Reached end of stream", e.getMessage());
            }

            Assert.assertFalse(client.isConnected());
            Assert.assertTrue(client.isClosed());

            // Now the second pair should start to work
            Assert.assertFalse(server2.isConnected());
            Assert.assertNull(server2.read(connectTimeout));
            Assert.assertTrue(server2.isConnected());

            client2.send("randomBytes".getBytes());
            Assert.assertNotNull(server2.read());
            Assert.assertTrue(client2.isConnected());
            Assert.assertTrue(server2.isConnected());

            client2.close();
            server2.close();
        }
    }

    @Test
    public void multiConnectTest() throws Exception {

        // The thread will be always on!
        @SuppressWarnings("resource")
        final Thread serverThread = new Thread(() -> {
            try {
                TCPSocket server = TCPSocket.asServer(TCPSocketTest.TEST_PORT);
                while (true) {
                    try {
                        System.out.println(new String(server.read()));
                    } catch (final Exception e) {
                        server.close();
                        if (!Thread.currentThread().isInterrupted()) {
                            // Rebuild!
                            server = TCPSocket.asServer(TCPSocketTest.TEST_PORT);
                        } else {
                            break;
                        }
                    }
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        });

        try (
                final TCPSocket clientSocket = TCPSocket.asClient("127.0.0.1", TCPSocketTest.TEST_PORT);
                final TCPSocket clientSocket2 = TCPSocket.asClient("127.0.0.1", TCPSocketTest.TEST_PORT)) {
            // the client socket won't be able to connect THIS fast
            clientSocket.waitUntilConnected(1);
            Assert.assertFalse("Client should not be able to open yet", clientSocket.isConnected());
            Assert.assertFalse("Client should not closed yet", clientSocket.isClosed());

            serverThread.start();

            // But given some more time it will....
            clientSocket.waitUntilConnected();
            Assert.assertNull("Client should not be able to read", clientSocket.read(100));
            Assert.assertTrue("Client should be connected by now", clientSocket.isConnected());
            Assert.assertFalse("Client should not closed yet", clientSocket.isClosed());

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
            Assert.assertFalse("Client should not be able to connect while it is taken", clientSocket2.isConnected());
            Assert.assertFalse("Client should not closed yet", clientSocket2.isClosed());

            try {
                clientSocket2.send("Some stuff".getBytes());
                Assert.fail();
            } catch (final Exception e) {
                Assert.assertEquals(NotYetConnectedException.class, e.getClass());
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
                Thread.sleep(100);
            } else {
                clientSocket.close(); // Same effect
            }
            Assert.assertFalse(clientSocket.isConnected());
            Assert.assertTrue(clientSocket.isClosed());

            try {
                clientSocket.send("Some stuff".getBytes());
                Assert.fail();
            } catch (final Exception e) {
                Assert.assertEquals(ClosedChannelException.class, e.getClass());
            }

            clientSocket2.waitUntilConnected();
            Assert.assertTrue(clientSocket2.isConnected());
            client2.interrupt(); // Will make sure clientSocket2 is closed
            serverThread.interrupt();
            Thread.sleep(200);

            Assert.assertFalse(clientSocket2.isConnected());
            Assert.assertTrue(clientSocket2.isClosed());
        }
    }

}
