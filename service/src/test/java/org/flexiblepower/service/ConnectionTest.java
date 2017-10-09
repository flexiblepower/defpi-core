/**
 * File ConnectionTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import org.flexiblepower.exceptions.SerializationException;
import org.flexiblepower.proto.ConnectionProto.ConnectionHandshake;
import org.flexiblepower.proto.ConnectionProto.ConnectionMessage;
import org.flexiblepower.proto.ConnectionProto.ConnectionState;
import org.flexiblepower.proto.ServiceProto.ErrorMessage;
import org.flexiblepower.serializers.JavaIOSerializer;
import org.flexiblepower.serializers.MessageSerializer;
import org.flexiblepower.serializers.ProtobufMessageSerializer;
import org.flexiblepower.service.TestService.TestServiceConfiguration;
import org.flexiblepower.service.exceptions.ServiceInvocationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

import com.google.protobuf.Message;

/**
 * ConnectionTest
 *
 * @author coenvl
 * @version 0.1
 * @since May 12, 2017
 */
// @Ignore // TODO These tests run fine from Eclipse, but not from maven.
public class ConnectionTest {

    private static final int IN_RECEIVE_TIMEOUT = 1000;
    private static final int OUT_SEND_TIMEOUT = 1000;
    private static final int MANAGEMENT_READ_TIMEOUT = 5000;

    private final static Logger logger = LoggerFactory.getLogger(ConnectionTest.class);

    private static final String CONNECTION_ID = "1";
    private static final String WRONG_CONNECTION_ID = "8";
    // private static final String TEST_HOST = "172.17.0.2";
    private static final String TEST_HOST = "localhost";

    private static final int TEST_SERVICE_LISTEN_PORT = 5020;
    private static final int TEST_SERVICE_TARGET_PORT = 5025;

    private TestService testService;
    private ServiceManager<TestServiceConfiguration> manager;
    private ZMQ.Socket managementSocket;

    private ServerSocket s;
    private Socket out;
    private Socket in;
    private OutputStream os;
    private InputStream is;
    private ProtobufMessageSerializer serializer;
    private Context ctx;

    @Before
    public void initConnection() throws Exception {
        this.testService = new TestService();
        this.manager = new ServiceManager<>(this.testService);

        ConnectionManager.registerConnectionHandlerFactory(TestService.class, this.testService);

        this.serializer = new ProtobufMessageSerializer();
        this.serializer.addMessageClass(ConnectionHandshake.class);
        this.serializer.addMessageClass(ConnectionMessage.class);
        this.serializer.addMessageClass(ErrorMessage.class);

        final String managementURI = String
                .format("tcp://%s:%d", ConnectionTest.TEST_HOST, ServiceManager.MANAGEMENT_PORT);
        this.ctx = ZMQ.context(1);

        this.managementSocket = this.ctx.socket(ZMQ.REQ);
        this.managementSocket.setImmediate(false);
        this.managementSocket.setReceiveTimeOut(ConnectionTest.MANAGEMENT_READ_TIMEOUT);

        this.managementSocket.connect(managementURI.toString());

        final String hostOfTestRunner = "127.0.0.1"; // InetAddress.getLocalHost().getCanonicalHostName();

        final ConnectionMessage createMsg = ConnectionMessage.newBuilder()
                .setConnectionId(ConnectionTest.CONNECTION_ID)
                .setMode(ConnectionMessage.ModeType.CREATE)
                .setTargetAddress("tcp://" + hostOfTestRunner + ":" + ConnectionTest.TEST_SERVICE_TARGET_PORT)
                .setListenPort(ConnectionTest.TEST_SERVICE_LISTEN_PORT)
                .setReceiveHash("eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252")
                .setSendHash("eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252")
                .build();

        Assert.assertTrue(this.managementSocket.send(this.serializer.serialize(createMsg)));

        final byte[] response = this.managementSocket.recv();
        final Message msg = this.serializer.deserialize(response);
        if (msg instanceof ErrorMessage) {
            Assert.fail("Error message received: " + ((ErrorMessage) msg).getDebugInformation());
        }

        final ConnectionHandshake message = (ConnectionHandshake) msg;
        Assert.assertNotNull(message);
        Assert.assertEquals(ConnectionState.STARTING, message.getConnectionState());

        // final String serviceURI = String
        // .format("tcp://%s:%d", ConnectionTest.TEST_HOST, ConnectionTest.TEST_SERVICE_LISTEN_PORT);
        this.out = new Socket(ConnectionTest.TEST_HOST, ConnectionTest.TEST_SERVICE_LISTEN_PORT);
        this.os = this.out.getOutputStream();
        // this.out = this.ctx.socket(ZMQ.PUB);
        // this.out.setSendTimeOut(ConnectionTest.OUT_SEND_TIMEOUT);

        // this.out.setImmediate(false);
        // this.out.connect(serviceURI.toString());

        final String listenURI = String.format("tcp://*:%d", ConnectionTest.TEST_SERVICE_TARGET_PORT);
        ConnectionTest.logger.info("Listening on {}", listenURI);
        this.s = new ServerSocket(ConnectionTest.TEST_SERVICE_TARGET_PORT);
        ConnectionTest.logger.info("{}", this.s);
        this.in = this.s.accept();
        ConnectionTest.logger.info("{}", this.in);
        this.is = this.in.getInputStream();
        // this.in = this.ctx.socket(ZMQ.SUB);
        // this.in.subscribe("");
        // this.in.setReceiveTimeOut(ConnectionTest.IN_RECEIVE_TIMEOUT);
        // this.in.bind(listenURI.toString());

        this.testAck();
    }

    /**
     * No longer a test, this MUST be sent by the socket before the connection will continue;
     */
    public void testAck() throws Exception {
        // We should receive a handshake from the other side, but the test service is NOT connected
        final byte[] recv = this.readSocketFilterHeartbeat();
        Assert.assertEquals(ConnectionHandshake.class, this.serializer.deserialize(recv).getClass());
        Assert.assertNotEquals("connected", this.testService.getState());

        // Send an ACK, but an incorrect one
        final ConnectionHandshake wrongHandshake = ConnectionHandshake.newBuilder()
                .setConnectionId(ConnectionTest.WRONG_CONNECTION_ID)
                .setConnectionState(ConnectionState.STARTING)
                .build();

        this.write(this.serializer.serialize(wrongHandshake));

        Assert.assertNotEquals("connected", this.testService.getState());

        // Send the real ack
        final ConnectionHandshake correctHandshake = ConnectionHandshake.newBuilder()
                .setConnectionId(ConnectionTest.CONNECTION_ID)
                .setConnectionState(ConnectionState.STARTING)
                .build();

        this.write(this.serializer.serialize(correctHandshake));

        // The other guy already sent an ack and was waiting for us, now it should continue;
        Thread.sleep(200);
        Assert.assertEquals("connected", this.testService.getState());
    }

    /**
     * @param serialize
     * @throws IOException
     */
    private void write(final byte[] msg) throws IOException {
        ConnectionTest.logger.trace("sending {} + {} = {} bytes ", msg.length / 256, msg.length % 256, msg.length);
        this.os.write(msg.length / 256);
        this.os.write(msg.length % 256);
        this.os.write(msg);
        this.os.write(0xFF);
        this.os.flush();
    }

    private byte[] readSocketFilterHeartbeat() throws IOException {
        byte[] data = this.readRaw();

        if (data != null) {
            while (data.length == 1) {
                ConnectionTest.logger.debug("Received heartbeat!");
                data = this.readRaw();
            }
            ConnectionTest.logger.info("Received: {}", new String(data));
        }
        return data;
    }

    private byte[] readRaw() throws IOException {
        final int len = (this.is.read() * 256) + this.is.read();
        ConnectionTest.logger.trace("Reading {} bytes from input stream", len);

        final byte[] data = new byte[len];
        final int read = this.is.read(data);
        if (read != len) {
            ConnectionTest.logger.warn("Expected {} bytes, only received {}", len, read);
        }
        int eof = this.is.read();
        if (eof != 0xFF) {
            ConnectionTest.logger.warn("Expected EOF, skipping stream");
            while (eof != 0xFF) {
                eof = this.is.read();
            }
        }
        return data;
    }

    @Test(timeout = 10000)
    public void
            testSend() throws InterruptedException, SerializationException, ServiceInvocationException, IOException {
        this.testService.resetCount();
        final int numTests = 100;
        final MessageSerializer<Serializable> serializer = new JavaIOSerializer();
        for (int i = 0; i < numTests; i++) {
            this.write(serializer.serialize("THIS IS A TEST " + i));
        }

        Thread.sleep(1000);
        Assert.assertEquals(numTests, this.testService.getCounter());
    }

    @Test(timeout = 10000)
    public void
            testSuspend() throws SerializationException, InterruptedException, ServiceInvocationException, IOException {
        Assert.assertNotEquals("connection-suspended", this.testService.getState());
        Assert.assertTrue(this.managementSocket.send(this.serializer.serialize(ConnectionMessage.newBuilder()
                .setConnectionId(ConnectionTest.CONNECTION_ID)
                .setMode(ConnectionMessage.ModeType.SUSPEND)
                .build())));

        // Make sure we get the correct response
        byte[] recv = this.managementSocket.recv();
        ConnectionHandshake acknowledgement = (ConnectionHandshake) this.serializer.deserialize(recv);
        Assert.assertEquals(ConnectionState.SUSPENDED, acknowledgement.getConnectionState());
        Thread.sleep(10);
        Assert.assertEquals("connection-suspended", this.testService.getState());

        this.os.close();
        this.out.close();

        final String hostOfTestRunner = InetAddress.getLocalHost().getCanonicalHostName();
        Assert.assertTrue(this.managementSocket.send(this.serializer.serialize(ConnectionMessage.newBuilder()
                .setConnectionId(ConnectionTest.CONNECTION_ID)
                .setMode(ConnectionMessage.ModeType.RESUME)
                .setTargetAddress("tcp://" + hostOfTestRunner + ":" + ConnectionTest.TEST_SERVICE_TARGET_PORT)
                .setListenPort(ConnectionTest.TEST_SERVICE_LISTEN_PORT)
                .setReceiveHash("eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252")
                .setSendHash("eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252")
                .build())));

        this.out = new Socket(ConnectionTest.TEST_HOST, ConnectionTest.TEST_SERVICE_LISTEN_PORT);
        this.os = this.out.getOutputStream();
        // this.out = this.ctx.socket(ZMQ.PUB);
        // this.out.setSendTimeOut(ConnectionTest.OUT_SEND_TIMEOUT);
        // this.out.setImmediate(false);

        final String serviceURI = String
                .format("tcp://%s:%d", ConnectionTest.TEST_HOST, ConnectionTest.TEST_SERVICE_LISTEN_PORT);

        // this.out.connect(serviceURI.toString());

        Thread.sleep(100);
        recv = this.managementSocket.recv();
        final Message msg = this.serializer.deserialize(recv);
        if (msg instanceof ErrorMessage) {
            Assert.fail("Received error message: " + ((ErrorMessage) msg).getDebugInformation());
        }
        acknowledgement = (ConnectionHandshake) msg;
        Assert.assertEquals(ConnectionState.CONNECTED, acknowledgement.getConnectionState());

        Thread.sleep(100);

        // We need to receive and send a handshake before the connection is in teh CONNECTED state again
        Assert.assertTrue(this.serializer.deserialize(this.readSocketFilterHeartbeat()) instanceof ConnectionHandshake);

        final ConnectionHandshake correctHandshake = ConnectionHandshake.newBuilder()
                .setConnectionId(ConnectionTest.CONNECTION_ID)
                .setConnectionState(ConnectionState.STARTING)
                .build();

        this.write(this.serializer.serialize(correctHandshake));

        Thread.sleep(1000);

        Assert.assertEquals("connection-resumed", this.testService.getState());
    }

    @Test(timeout = 10000)
    public void testTerminate() throws SerializationException,
            InterruptedException,
            UnknownHostException,
            ServiceInvocationException {
        Assert.assertNotEquals("connection-terminated", this.testService.getState());
        Assert.assertTrue(this.managementSocket.send(this.serializer.serialize(ConnectionMessage.newBuilder()
                .setConnectionId(ConnectionTest.CONNECTION_ID)
                .setMode(ConnectionMessage.ModeType.TERMINATE)
                .build())));
        final byte[] recv = this.managementSocket.recv();
        final ConnectionHandshake acknowledgement = (ConnectionHandshake) this.serializer.deserialize(recv);
        Assert.assertEquals(ConnectionState.TERMINATED, acknowledgement.getConnectionState());
        Thread.sleep(1000); // Terminate method is call asynchronous
        Assert.assertEquals("connection-terminated", this.testService.getState());
    }

    @After()
    public void closeConnection() throws InterruptedException, IOException {
        if (this.manager != null) {
            this.manager.close();
            this.manager = null;
        }
        if (this.out != null) {
            this.os.close();
            this.out.close();
            this.out = null;
        }
        if (this.in != null) {
            this.is.close();
            this.in.close();
            this.s.close();
            this.in = null;
        }
        Thread.sleep(200);
    }

}
