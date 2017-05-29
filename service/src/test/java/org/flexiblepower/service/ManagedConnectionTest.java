/**
 * File ManagedConnectionTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import org.flexiblepower.service.exceptions.ConnectionModificationException;
import org.junit.Assert;
import org.junit.Test;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

/**
 * ManagedConnectionTest
 *
 * @author coenvl
 * @version 0.1
 * @since May 29, 2017
 */
public class ManagedConnectionTest {

    @Test
    public void testConnection() throws ConnectionModificationException {
        final ManagedConnection conn = new ManagedConnection(1234, "tcp://localhost:5678", new TestService());
    }

    @Test
    public void testZMQ() throws ConnectionModificationException {
        final Context zmqContext = ZMQ.context(1);
        final Socket publishSocket = zmqContext.socket(ZMQ.PUSH);
        publishSocket.setDelayAttachOnConnect(true);
        publishSocket.connect("tcp://localhost:23456");
        // publishSocket.bindToRandomPort("tcp://*");
        publishSocket.setSendTimeOut(1000);
        Assert.assertFalse(publishSocket.send("TEST".getBytes()));
    }

}
