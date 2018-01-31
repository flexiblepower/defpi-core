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

import java.util.Arrays;
import java.util.List;

import org.flexiblepower.commons.TCPSocket;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * ConnectionTest
 *
 * @version 0.1
 * @since May 12, 2017
 */
@SuppressWarnings("static-method")
@RunWith(Parameterized.class)
public class TCPSocketTest {

    @Parameters
    public static List<Object[]> data() {
        return Arrays.asList(new Object[3][0]);
    }

    @Test
    public void doTest() throws Exception {
        try (
                final TCPSocket client = TCPSocket.asClient("127.0.0.1", 5000);
                final TCPSocket server = TCPSocket.asServer(5000)) {
            client.waitUntilConnected(0);
            client.send("Test data".getBytes());
            server.waitUntilConnected(0);
            System.out.println(new String(server.read()));
        }
    }

    @Test
    public void testTimeout() throws Exception {
        try (
                final TCPSocket client = TCPSocket.asClient("127.0.0.1", 5000);
                final TCPSocket server = TCPSocket.asServer(5000)) {
            client.waitUntilConnected(0);
            long t_start = System.currentTimeMillis();
            Assert.assertNull(server.read(100));
            long t_wait = System.currentTimeMillis() - t_start;
            System.out.format("I had to wait %d ms\n", t_wait);
            Assert.assertTrue(Math.abs(t_wait - 100) < 25);

            t_start = System.currentTimeMillis();
            Assert.assertNull(server.read(200));
            t_wait = System.currentTimeMillis() - t_start;
            System.out.format("I had to wait %d ms\n", t_wait);
            Assert.assertTrue(Math.abs(t_wait - 200) < 25);

            client.send("Test data".getBytes());
            Assert.assertEquals("Test data", new String(server.read()));
        }
    }

    @Test
    public void doTest2() throws Exception {
        try (
                final TCPSocket client = TCPSocket.asClient("127.0.0.1", 5000);
                final TCPSocket server = TCPSocket.asServer(5000)) {
            client.waitUntilConnected(0);
            client.send("MORE ! Test data".getBytes());
            server.waitUntilConnected(0);
            System.out.println(new String(server.read()));
        }
    }

    @Test
    public void multiConnectTest() throws Exception {
        final Thread serverThread = new Thread(() -> {
            try {
                TCPSocket server = TCPSocket.asServer(5001);
                while (true) {
                    try {
                        server.waitUntilConnected(0);
                        // System.out.println(new String(server.read()));
                    } catch (final Exception e) {
                        // e.printStackTrace();
                        server.close();
                        server = TCPSocket.asServer(5001);
                    }
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        });
        serverThread.start();
        Thread.sleep(100);

        final Thread client1 = new Thread(() -> {
            try (TCPSocket client = TCPSocket.asClient("127.0.0.1", 5001)) {
                client.waitUntilConnected(0);
                while (true) {
                    client.send("This is a test".getBytes());
                    Thread.sleep(100);
                }
            } catch (final Exception e) {
                // e.printStackTrace();
            }
        });
        client1.start();
        Thread.sleep(100);

        final Thread client2 = new Thread(() -> {
            TCPSocket client = TCPSocket.asClient("127.0.0.1", 5001);
            while (true) {
                try {
                    client.waitUntilConnected(0);
                    client.send("This is the second test".getBytes());
                    Thread.sleep(100);
                } catch (final Exception e) {
                    // e.printStackTrace();
                    client.close();
                    client = TCPSocket.asClient("127.0.0.1", 5001);
                }
            }
        });
        client2.start();
        Thread.sleep(100);

        client1.interrupt();
        Thread.sleep(100);
        client2.interrupt();
    }

    @After
    public void cleanup() throws InterruptedException {
        TCPSocket.destroyLingeringSockets();
    }

}
