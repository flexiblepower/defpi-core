/**
 * File ConnectionTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.io.IOException;

import org.junit.Test;

/**
 * ConnectionTest
 *
 * @author coenvl
 * @version 0.1
 * @since May 12, 2017
 */
public class TCPSocketTest {

    @Test
    public void doTest() throws InterruptedException, IOException {
        final TCPSocket client = TCPSocket.asClient("127.0.0.1", 5000);
        final TCPSocket server = TCPSocket.asServer(5000);
        client.send("Test data".getBytes());
        System.out.println(new String(server.read()));
    }

}
