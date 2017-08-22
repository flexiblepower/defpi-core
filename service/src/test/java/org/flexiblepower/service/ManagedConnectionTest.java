/**
 * File ManagedConnectionTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import org.flexiblepower.proto.ConnectionProto.ConnectionState;
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

    @Test(timeout = 5000)
    public void testConnection() throws ConnectionModificationException {
        final ManagedConnection conn = new ManagedConnection("ConnID1234",
                1234,
                "tcp://localhost:5678",
                TestService.class.getAnnotation(InterfaceInfo.class));
        Assert.assertEquals(ConnectionState.STARTING, conn.getState());
        conn.goToTerminatedState();
        Assert.assertEquals(ConnectionState.TERMINATED, conn.getState());
    }

    @Test(timeout = 5000)
    public void testZMQ() throws ConnectionModificationException {
        final Context zmqContext = ZMQ.context(1);
        final Socket publishSocket = zmqContext.socket(ZMQ.PUSH);
        publishSocket.setImmediate(false);
        publishSocket.connect("tcp://localhost:23456");
        publishSocket.setSendTimeOut(100);

        Assert.assertFalse(publishSocket.send("TEST".getBytes()));
        publishSocket.close();
        zmqContext.close();
    }

}
