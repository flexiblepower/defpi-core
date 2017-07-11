/**
 * File ConnectionTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.flexiblepower.exceptions.SerializationException;
import org.flexiblepower.proto.ConnectionProto.ConnectionHandshake;
import org.flexiblepower.proto.ConnectionProto.ConnectionMessage;
import org.flexiblepower.proto.ConnectionProto.ConnectionState;
import org.flexiblepower.serializers.JavaIOSerializer;
import org.flexiblepower.serializers.ProtobufMessageSerializer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * ConnectionTest
 *
 * @author coenvl
 * @version 0.1
 * @since May 12, 2017
 */
public class ConnectionTest {

    /**
     *
     */
    private static final String CONNECTION_ID = "1";
    private static final String WRONG_CONNECTION_ID = "8";
    // private static final String TEST_HOST = "172.17.0.2";
    private static final String TEST_HOST = "localhost";
    private static final int TEST_SERVICE_LISTEN_PORT = 5020;
    private static final int TEST_SERVICE_TARGET_PORT = 5025;

    private TestService testService;
    private ServiceManager manager;
    private Socket managementSocket;

    private Socket out;
    private Socket in;
    private ProtobufMessageSerializer serializer;

    @Before
    public void initConnection() throws UnknownHostException, InterruptedException {
        this.testService = new TestService();
        this.manager = new ServiceManager(this.testService);
        this.serializer = new ProtobufMessageSerializer();
        this.serializer.addMessageClass(ConnectionHandshake.class);

        final String managementURI = String.format("tcp://%s:%d",
                ConnectionTest.TEST_HOST,
                ServiceManager.MANAGEMENT_PORT);
        final Context ctx = ZMQ.context(1);
        this.managementSocket = ctx.socket(ZMQ.REQ);
        this.managementSocket.setReceiveTimeOut(1000);
        this.managementSocket.connect(managementURI.toString());

        final String hostOfTestRunner = InetAddress.getLocalHost().getCanonicalHostName();

        final ConnectionMessage createMsg = ConnectionMessage.newBuilder()
                .setConnectionId(ConnectionTest.CONNECTION_ID)
                .setMode(ConnectionMessage.ModeType.CREATE)
                .setTargetAddress("tcp://" + hostOfTestRunner + ":" + ConnectionTest.TEST_SERVICE_TARGET_PORT)
                .setListenPort(ConnectionTest.TEST_SERVICE_LISTEN_PORT)
                .setReceiveHash("eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252")
                .setSendHash("eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252")
                .build();

        Assert.assertTrue(this.managementSocket.send(createMsg.toByteArray()));
        final byte[] response = this.managementSocket.recv();
        ConnectionHandshake message = null;
        try {
            message = (ConnectionHandshake) this.serializer.deserialize(response);
        } catch (final SerializationException e) {
            System.out.println("Message: " + response);
            e.printStackTrace();
        }
        Assert.assertNotNull(message);
        Assert.assertEquals(ConnectionState.STARTING, message.getConnectionState());

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
    public void testAck() throws InterruptedException, SerializationException {
        // Now start real tests, first send random string
        Assert.assertTrue("Failed to send random string", this.out.send("This is just a not an ack"));
        Assert.assertNull("Random string should not be answered", this.in.recv());

        // Send an ACK, but an incorrect one
        final ConnectionHandshake wrongHandshake = ConnectionHandshake.newBuilder()
                .setConnectionId(ConnectionTest.WRONG_CONNECTION_ID)
                .setConnectionState(ConnectionState.STARTING)
                .build();

        Assert.assertTrue("Failed to send wrong ACK",
                this.out.send(new String(this.serializer.serialize(wrongHandshake))));
        Assert.assertNull("Wrong ACK should not be answered", this.in.recv());

        // Send the real ack
        final ConnectionHandshake correctHandshake = ConnectionHandshake.newBuilder()
                .setConnectionId(ConnectionTest.CONNECTION_ID)
                .setConnectionState(ConnectionState.STARTING)
                .build();

        Assert.assertNotEquals("connected", this.testService.getState());
        final String handShakeString = new String(this.serializer.serialize(correctHandshake));

        Assert.assertTrue("Sending real ACK", this.out.send(handShakeString));
        Thread.sleep(500);
        Assert.assertEquals("connected", this.testService.getState());
        Assert.assertNotNull(this.in.recv());

        Assert.assertTrue("Failed to send second ACK", this.out.send(handShakeString));
        try {
            ConnectionHandshake.parseFrom(this.in.recv());
            Assert.fail("Expected exception");
        } catch (final Exception e) {
            Assert.assertEquals(InvalidProtocolBufferException.class, e.getClass());
        }
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
    public void testSuspend() throws SerializationException {
        Assert.assertNotEquals("connection-suspended", this.testService.getState());
        Assert.assertTrue(this.managementSocket.send(ConnectionMessage.newBuilder()
                .setConnectionId(ConnectionTest.CONNECTION_ID)
                .setMode(ConnectionMessage.ModeType.SUSPEND)
                .build()
                .toByteArray()));

        byte[] recv = this.managementSocket.recv();
        ConnectionHandshake acknowledgement = (ConnectionHandshake) this.serializer.deserialize(recv);
        Assert.assertEquals(ConnectionState.SUSPENDED, acknowledgement.getConnectionState());
        Assert.assertEquals("connection-suspended", this.testService.getState());

        Assert.assertTrue(this.managementSocket.send(ConnectionMessage.newBuilder()
                .setConnectionId(ConnectionTest.CONNECTION_ID)
                .setMode(ConnectionMessage.ModeType.RESUME)
                .build()
                .toByteArray()));
        recv = this.managementSocket.recv();
        acknowledgement = (ConnectionHandshake) this.serializer.deserialize(recv);
        Assert.assertEquals(ConnectionState.CONNECTED, acknowledgement.getConnectionState());
        Assert.assertEquals("connection-resumed", this.testService.getState());
    }

    @Test(timeout = 5000)
    public void testTerminate() throws SerializationException {
        Assert.assertNotEquals("connection-terminated", this.testService.getState());
        Assert.assertTrue(this.managementSocket.send(ConnectionMessage.newBuilder()
                .setConnectionId(ConnectionTest.CONNECTION_ID)
                .setMode(ConnectionMessage.ModeType.TERMINATE)
                .build()
                .toByteArray()));
        final byte[] recv = this.managementSocket.recv();
        final ConnectionHandshake acknowledgement = (ConnectionHandshake) this.serializer.deserialize(recv);
        Assert.assertEquals(ConnectionState.TERMINATED, acknowledgement.getConnectionState());
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
