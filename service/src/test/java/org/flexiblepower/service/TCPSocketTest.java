/**
 * File ConnectionTest.java
 *
 * Copyright 2017 FAN
 */
package org.flexiblepower.service;

import org.flexiblepower.commons.TCPSocket;
import org.junit.Test;

/**
 * ConnectionTest
 *
 * @version 0.1
 * @since May 12, 2017
 */
@SuppressWarnings("static-method")
public class TCPSocketTest {

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

}
