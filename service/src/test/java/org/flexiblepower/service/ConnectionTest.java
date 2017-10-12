/**
 * File ConnectionTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
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

    private static final int MANAGEMENT_READ_TIMEOUT = 5000;

    private final static Logger logger = LoggerFactory.getLogger(ConnectionTest.class);

    private static final String CONNECTION_ID = "1";
    private static final String WRONG_CONNECTION_ID = "8";
    // private static final String TEST_HOST = "172.17.0.2";

    private static final int TEST_SERVICE_LISTEN_PORT = 5020;

    private TestService testService;
    private ServiceManager<TestServiceConfiguration> manager;
    private ZMQ.Socket managementSocket;

    private TCPSocket socket;

    private ProtobufMessageSerializer serializer;
    private Context ctx;

    @Before
    public void initConnection() throws Exception {
        ConnectionTest.logger.info("*** Start of test ***");
        this.testService = new TestService();
        this.manager = new ServiceManager<>(this.testService);

        ConnectionManager.registerConnectionHandlerFactory(TestService.class, this.testService);

        this.serializer = new ProtobufMessageSerializer();
        this.serializer.addMessageClass(ConnectionHandshake.class);
        this.serializer.addMessageClass(ConnectionMessage.class);
        this.serializer.addMessageClass(ErrorMessage.class);

        final String hostOfTestRunner = InetAddress.getLocalHost().getCanonicalHostName();
        final String managementURI = String.format("tcp://%s:%d", hostOfTestRunner, ServiceManager.MANAGEMENT_PORT);
        this.ctx = ZMQ.context(1);

        this.managementSocket = this.ctx.socket(ZMQ.REQ);
        this.managementSocket.setImmediate(false);
        this.managementSocket.setReceiveTimeOut(ConnectionTest.MANAGEMENT_READ_TIMEOUT);

        this.managementSocket.connect(managementURI.toString());

        final ConnectionMessage createMsg = ConnectionMessage.newBuilder()
                .setConnectionId(ConnectionTest.CONNECTION_ID)
                .setMode(ConnectionMessage.ModeType.CREATE)
                .setTargetAddress("")
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

        // Create our local socket to connect to
        this.socket = TCPSocket.asClient(hostOfTestRunner, ConnectionTest.TEST_SERVICE_LISTEN_PORT);

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

        this.socket.send(this.serializer.serialize(wrongHandshake));

        Assert.assertNotEquals("connected", this.testService.getState());

        // Send the real ack
        final ConnectionHandshake correctHandshake = ConnectionHandshake.newBuilder()
                .setConnectionId(ConnectionTest.CONNECTION_ID)
                .setConnectionState(ConnectionState.CONNECTED)
                .build();

        this.socket.send(this.serializer.serialize(correctHandshake));

        // The other guy already sent an ack and was waiting for us, now it should continue;
        Thread.sleep(1000);
        Assert.assertEquals("connected", this.testService.getState());
    }

    @Test(timeout = 10000)
    public void
            testSend() throws InterruptedException, SerializationException, ServiceInvocationException, IOException {
        this.testService.resetCount();
        final int numTests = 100;
        final MessageSerializer<Serializable> serializer = new JavaIOSerializer();
        for (int i = 0; i < numTests; i++) {
            this.socket.send(serializer.serialize("THIS IS A TEST " + i));
        }

        Thread.sleep(1000);
        Assert.assertEquals(numTests, this.testService.getCounter());
    }

    @Test // (timeout = 10000)
    public void
            testSuspend() throws SerializationException, InterruptedException, ServiceInvocationException, IOException {
        Assert.assertNotEquals("connection-suspended", this.testService.getState());
        Assert.assertTrue(this.managementSocket.send(this.serializer.serialize(ConnectionMessage.newBuilder()
                .setConnectionId(ConnectionTest.CONNECTION_ID)
                .setMode(ConnectionMessage.ModeType.SUSPEND)
                .build())));

        // Make sure we get the correct response
        final byte[] recv = this.managementSocket.recv();
        ConnectionHandshake acknowledgement = (ConnectionHandshake) this.serializer.deserialize(recv);
        Assert.assertEquals(ConnectionState.SUSPENDED, acknowledgement.getConnectionState());
        Thread.sleep(10);
        Assert.assertEquals("connection-suspended", this.testService.getState());

        // Make a new connection TO the test service
        this.socket.close();

        // Wait for at least one potential heartbeat that should NOT properly go
        Thread.sleep(4000);

        final String hostOfTestRunner = InetAddress.getLocalHost().getCanonicalHostName();
        Assert.assertTrue(this.managementSocket.send(this.serializer.serialize(ConnectionMessage.newBuilder()
                .setConnectionId(ConnectionTest.CONNECTION_ID)
                .setMode(ConnectionMessage.ModeType.RESUME)
                .setTargetAddress("")
                .setListenPort(ConnectionTest.TEST_SERVICE_LISTEN_PORT)
                .setReceiveHash("eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252")
                .setSendHash("eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252")
                .build())));

        this.socket = TCPSocket.asClient(hostOfTestRunner, ConnectionTest.TEST_SERVICE_LISTEN_PORT);

        Thread.sleep(100);

        final Message msg = this.serializer.deserialize(this.managementSocket.recv());
        if (msg instanceof ErrorMessage) {
            Assert.fail("Received error message: " + ((ErrorMessage) msg).getDebugInformation());
        }
        acknowledgement = (ConnectionHandshake) msg;
        Assert.assertEquals(ConnectionState.CONNECTED, acknowledgement.getConnectionState());

        Thread.sleep(1000);

        // We need to receive and send a handshake before the connection is in the CONNECTED state again
        final Object receivedHandshake = this.serializer.deserialize(this.readSocketFilterHeartbeat());
        Assert.assertEquals(ConnectionHandshake.class, receivedHandshake.getClass());
        Assert.assertEquals(ConnectionState.SUSPENDED, ((ConnectionHandshake) receivedHandshake).getConnectionState());

        final ConnectionHandshake correctHandshake = ConnectionHandshake.newBuilder()
                .setConnectionId(ConnectionTest.CONNECTION_ID)
                .setConnectionState(ConnectionState.CONNECTED)
                .build();

        this.socket.send(this.serializer.serialize(correctHandshake));

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
        Thread.sleep(100); // Terminate method is call asynchronous
        Assert.assertEquals("connection-terminated", this.testService.getState());
    }

    @After()
    public void closeConnection() throws InterruptedException, IOException {
        ConnectionTest.logger.info("*** END-OF-TEST ***");
        if (this.manager != null) {
            this.manager.close();
            this.manager = null;
        }
        if (this.socket != null) {
            this.socket.close();
            this.socket = null;
        }
        Thread.sleep(200);
    }

    private byte[] readSocketFilterHeartbeat() throws IOException, InterruptedException {
        byte[] data = this.socket.read();

        if (data != null) {
            while (data.length == 1) {
                ConnectionTest.logger.debug("Received heartbeat!");
                data = this.socket.read();
            }
            ConnectionTest.logger.info("Received: {}", new String(data));
        }
        return data;
    }

}
