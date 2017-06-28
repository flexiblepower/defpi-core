/**
 * File ConnectionTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.flexiblepower.exceptions.SerializationException;
import org.flexiblepower.proto.ConnectionProto.ConnectionMessage;
import org.flexiblepower.serializers.JavaIOSerializer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

/**
 * ConnectionTest
 *
 * @author coenvl
 * @version 0.1
 * @since May 12, 2017
 */
public class ConnectionTest {

    // private static final String TEST_HOST = "172.17.0.2";
    private static final String TEST_HOST = "localhost";
    private static final int TEST_SERVICE_LISTEN_PORT = 5020;
    private static final int TEST_SERVICE_TARGET_PORT = 5025;

    private TestService testService;
    private ServiceManager manager;
    private Socket managementSocket;

    private Socket out;
    private Socket in;

    @Before
    public void initConnection() throws UnknownHostException, InterruptedException {
        this.testService = new TestService();
        this.manager = new ServiceManager(this.testService);

        final String managementURI = String.format("tcp://%s:%d",
                ConnectionTest.TEST_HOST,
                ServiceManager.MANAGEMENT_PORT);
        final Context ctx = ZMQ.context(1);
        this.managementSocket = ctx.socket(ZMQ.REQ);
        this.managementSocket.setReceiveTimeOut(1000);
        this.managementSocket.connect(managementURI.toString());

        final String hostOfTestRunner = InetAddress.getLocalHost().getCanonicalHostName();

        final ConnectionMessage createMsg = ConnectionMessage.newBuilder()
                .setConnectionId("1")
                .setMode(ConnectionMessage.ModeType.CREATE)
                .setTargetAddress("tcp://" + hostOfTestRunner + ":" + ConnectionTest.TEST_SERVICE_TARGET_PORT)
                .setListenPort(ConnectionTest.TEST_SERVICE_LISTEN_PORT)
                .setReceiveHash("eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252")
                .setSendHash("eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252")
                .build();

        Assert.assertTrue(this.managementSocket.send(createMsg.toByteArray()));
        Assert.assertArrayEquals(ServiceManager.SUCCESS, this.managementSocket.recv());

        final String serviceURI = String.format("tcp://%s:%d",
                ConnectionTest.TEST_HOST,
                ConnectionTest.TEST_SERVICE_LISTEN_PORT);
        this.out = ctx.socket(ZMQ.PUSH);
        this.out.setSendTimeOut(0);
        this.out.setDelayAttachOnConnect(true);
        this.out.connect(serviceURI.toString());

        final String listenURI = String.format("tcp://*:%d", ConnectionTest.TEST_SERVICE_TARGET_PORT);
        this.in = ctx.socket(ZMQ.PULL);
        this.in.setReceiveTimeOut(200);
        this.in.bind(listenURI.toString());
        Thread.sleep(500); // Allow remote thread to process the connection message
    }

    @Test(timeout = 5000)
    public void testAck() throws InterruptedException {
        // Now start real tests, first send random string
        Assert.assertTrue("Failed to send random string", this.out.send("This is just a not an ack"));
        Assert.assertNull("Random string should not be answered", this.in.recv());

        // Send an ACK, but an incorrect one
        final String wrongack = String.format("%s:%s/%s@%s",
                "@Defpi-0.2.1 connection ready",
                "eefc3942366e0b12795edb10f5358i45694e45a7a6e96144299ff2e1f8f5c252",
                "eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252",
                JavaIOSerializer.class);

        Assert.assertTrue("Failed to send wrong ACK", this.out.send(wrongack));
        Assert.assertNull("Wrong ACK should not be answered", this.in.recv());

        // Send the real ack
        final String ack = String.format("%s:%s/%s@%s",
                "@Defpi-0.2.1 connection ready",
                "eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252",
                "eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252",
                JavaIOSerializer.class);

        Assert.assertNotEquals("connected", this.testService.getState());
        Assert.assertTrue("Failed to send real ACK", this.out.send(ack));
        Assert.assertArrayEquals("Unexpected response to ACK", ack.getBytes(), this.in.recv());
        Assert.assertEquals("connected", this.testService.getState());

        // Send ANOTHER ack
        Assert.assertTrue("Failed to send second ACK", this.out.send(ack));
        Assert.assertNull("Second ACK should not bew answered", this.in.recv());
    }

    @Test(timeout = 5000)
    public void testSend() throws InterruptedException, SerializationException {
        this.testService.resetCount();

        final int numTests = 100;
        for (int i = 0; i < numTests; i++) {
            Assert.assertTrue(this.out.send((new JavaIOSerializer()).serialize("THIS IS A TEST " + i)));
        }

        Thread.sleep(1000);
        Assert.assertEquals(numTests, this.testService.getCounter());
    }

    @Test(timeout = 5000)
    public void testSuspend() {
        Assert.assertNotEquals("connection-suspended", this.testService.getState());
        Assert.assertTrue(this.managementSocket.send(ConnectionMessage.newBuilder()
                .setConnectionId("1")
                .setMode(ConnectionMessage.ModeType.SUSPEND)
                .build()
                .toByteArray()));
        Assert.assertArrayEquals(ServiceManager.SUCCESS, this.managementSocket.recv());
        Assert.assertEquals("connection-suspended", this.testService.getState());

        Assert.assertTrue(this.managementSocket.send(ConnectionMessage.newBuilder()
                .setConnectionId("1")
                .setMode(ConnectionMessage.ModeType.RESUME)
                .build()
                .toByteArray()));
        Assert.assertArrayEquals(ServiceManager.SUCCESS, this.managementSocket.recv());
        Assert.assertEquals("connection-resumed", this.testService.getState());
    }

    @Test(timeout = 5000)
    public void testTerminate() {
        Assert.assertNotEquals("connection-terminated", this.testService.getState());
        Assert.assertTrue(this.managementSocket.send(ConnectionMessage.newBuilder()
                .setConnectionId("1")
                .setMode(ConnectionMessage.ModeType.TERMINATE)
                .build()
                .toByteArray()));
        Assert.assertArrayEquals(ServiceManager.SUCCESS, this.managementSocket.recv());
        Assert.assertEquals("connection-terminated", this.testService.getState());
    }

    @After
    public void closeConnection() throws InterruptedException {
        if (this.manager != null) {
            this.manager.close();
            this.manager = null;
        }
        if (this.out != null) {
            this.out.close();
            this.out = null;
        }
        if (this.in != null) {
            this.in.close();
            this.in = null;
        }
        Thread.sleep(100);
    }

}
