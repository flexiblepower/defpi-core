/*-
 * #%L
 * dEF-Pi service managing library
 * %%
 * Copyright (C) 2017 - 2018 Flexible Power Alliance Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package org.flexiblepower.service;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import org.flexiblepower.commons.TCPSocket;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;

/**
 * ConnectionTest
 *
 * @version 0.1
 * @since May 12, 2017
 */
@SuppressWarnings("javadoc")
@Timeout(value = 10, unit = TimeUnit.SECONDS)
public class ConnectionTest {

    private final static Logger logger = LoggerFactory.getLogger(ConnectionTest.class);

    private static final String CONNECTION_ID = "1";
    private static final String WRONG_CONNECTION_ID = "8";
    // private static final String TEST_HOST = "172.17.0.2";

    private static final int TEST_SERVICE_LISTEN_PORT = 5020;

    private TestService testService;
    private ServiceManager<TestServiceConfiguration> manager;
    private TCPSocket managementSocket;
    private TCPSocket dataSocket;

    private ProtobufMessageSerializer serializer;

    @BeforeEach
    public void initConnection() throws Exception {
        ConnectionTest.logger.info("*** Start of test ***");
        this.testService = new TestService();
        this.manager = new ServiceManager<>();
        try {
            this.manager.start(this.testService);
        } catch (final Exception e) {
            // In some JREs this will be a NPE, in others it will be a runtime exception
            if (!(e instanceof NullPointerException)) {
                Assertions.assertEquals(RuntimeException.class, e.getClass());
                Assertions.assertEquals(IllegalArgumentException.class, e.getCause().getClass());
                Assertions.assertTrue(e.getMessage().contains("protocol = http host = null"));
            }
        }

        ConnectionManager.registerConnectionHandlerFactory(TestService.class, this.testService);

        this.serializer = new ProtobufMessageSerializer();
        this.serializer.addMessageClass(ConnectionHandshake.class);
        this.serializer.addMessageClass(ConnectionMessage.class);
        this.serializer.addMessageClass(ErrorMessage.class);

        final String hostOfTestRunner = InetAddress.getLocalHost().getCanonicalHostName();

        this.managementSocket = TCPSocket.asClient(hostOfTestRunner, ServiceManager.MANAGEMENT_PORT);
        this.managementSocket.waitUntilConnected();

        final ConnectionMessage createMsg = ConnectionMessage.newBuilder()
                .setConnectionId(ConnectionTest.CONNECTION_ID)
                .setMode(ConnectionMessage.ModeType.CREATE)
                .setTargetAddress("")
                .setListenPort(ConnectionTest.TEST_SERVICE_LISTEN_PORT)
                .setReceiveHash("eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252")
                .setSendHash("eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252")
                .build();

        this.managementSocket.send(this.serializer.serialize(createMsg));

        final byte[] response = this.managementSocket.read();
        final Message msg = this.serializer.deserialize(response);
        if (msg instanceof ErrorMessage) {
            Assertions.fail("Error message received: " + ((ErrorMessage) msg).getDebugInformation());
        }

        final ConnectionHandshake message = (ConnectionHandshake) msg;
        Assertions.assertNotNull(message);
        Assertions.assertEquals(ConnectionState.STARTING, message.getConnectionState());

        // Create our local socket to connect to
        this.dataSocket = TCPSocket.asClient(hostOfTestRunner, ConnectionTest.TEST_SERVICE_LISTEN_PORT);
        this.dataSocket.waitUntilConnected();
        this.testAck();
    }

    /**
     * No longer a test, this MUST be sent by the socket before the connection will continue;
     */
    public void testAck() throws Exception {
        // We should receive a handshake from the other side, but the test service is NOT connected
        final byte[] recv = this.readSocketFilterHeartbeat();
        Assertions.assertEquals(ConnectionHandshake.class, this.serializer.deserialize(recv).getClass());
        Assertions.assertTrue(this.testService.stateQueue.isEmpty());

        // Send an ACK, but an incorrect one
        final ConnectionHandshake wrongHandshake = ConnectionHandshake.newBuilder()
                .setConnectionId(ConnectionTest.WRONG_CONNECTION_ID)
                .setConnectionState(ConnectionState.STARTING)
                .build();

        this.dataSocket.send(this.serializer.serialize(wrongHandshake));
        Assertions.assertTrue(this.testService.stateQueue.isEmpty());

        // Send the real ack
        final ConnectionHandshake correctHandshake = ConnectionHandshake.newBuilder()
                .setConnectionId(ConnectionTest.CONNECTION_ID)
                .setConnectionState(ConnectionState.CONNECTED)
                .build();

        this.dataSocket.send(this.serializer.serialize(correctHandshake));

        // The other guy already sent an ack and was waiting for us, now it should continue;
        Assertions.assertEquals("connected", this.testService.stateQueue.take());
    }

    @RepeatedTest(3)
    public void
            testSend() throws InterruptedException, SerializationException, ServiceInvocationException, IOException {
        final int numTests = 100;
        this.testService.expect(numTests);
        final MessageSerializer<Serializable> javaSerializer = new JavaIOSerializer();
        for (int i = 0; i < numTests; i++) {
            this.dataSocket.send(javaSerializer.serialize("THIS IS A TEST " + i));
        }

        Assertions.assertNotNull(this.testService.stateQueue.take());
        Assertions.assertEquals(0, this.testService.getCounter());
    }

    @RepeatedTest(3)
    public void
            testSuspend() throws SerializationException, InterruptedException, ServiceInvocationException, IOException {
        Assertions.assertTrue(this.testService.stateQueue.isEmpty());
        this.managementSocket.send(this.serializer.serialize(ConnectionMessage.newBuilder()
                .setConnectionId(ConnectionTest.CONNECTION_ID)
                .setMode(ConnectionMessage.ModeType.SUSPEND)
                .build()));

        // Make sure we get the correct response
        final byte[] recv = this.managementSocket.read();
        ConnectionHandshake acknowledgement = (ConnectionHandshake) this.serializer.deserialize(recv);
        Assertions.assertEquals(ConnectionState.SUSPENDED, acknowledgement.getConnectionState());
        Assertions.assertEquals("connection-suspended", this.testService.stateQueue.take());

        this.managementSocket.send(this.serializer.serialize(ConnectionMessage.newBuilder()
                .setConnectionId(ConnectionTest.CONNECTION_ID)
                .setMode(ConnectionMessage.ModeType.RESUME)
                .setTargetAddress("")
                .setListenPort(ConnectionTest.TEST_SERVICE_LISTEN_PORT)
                .setReceiveHash("eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252")
                .setSendHash("eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252")
                .build()));

        final Message msg = this.serializer.deserialize(this.managementSocket.read());
        if (msg instanceof ErrorMessage) {
            Assertions.fail("Received error message: " + ((ErrorMessage) msg).getDebugInformation());
        }
        acknowledgement = (ConnectionHandshake) msg;
        Assertions.assertEquals(ConnectionState.CONNECTED, acknowledgement.getConnectionState());

        final String hostOfTestRunner = InetAddress.getLocalHost().getCanonicalHostName();
        // Make a new connection TO the test service
        this.dataSocket.close();
        this.dataSocket = TCPSocket.asClient(hostOfTestRunner, ConnectionTest.TEST_SERVICE_LISTEN_PORT);
        this.dataSocket.waitUntilConnected();
        // We need to receive and send a handshake before the connection is in the CONNECTED state again
        final Object receivedHandshake = this.serializer.deserialize(this.readSocketFilterHeartbeat());
        Assertions.assertEquals(ConnectionHandshake.class, receivedHandshake.getClass());
        Assertions.assertEquals(ConnectionState.SUSPENDED,
                ((ConnectionHandshake) receivedHandshake).getConnectionState());

        final ConnectionHandshake correctHandshake = ConnectionHandshake.newBuilder()
                .setConnectionId(ConnectionTest.CONNECTION_ID)
                .setConnectionState(ConnectionState.CONNECTED)
                .build();

        this.dataSocket.send(this.serializer.serialize(correctHandshake));

        Assertions.assertEquals("connection-resumed", this.testService.stateQueue.take());
    }

    @RepeatedTest(3)
    public void testTerminate() throws Exception {
        Assertions.assertTrue(this.testService.stateQueue.isEmpty());
        this.managementSocket.send(this.serializer.serialize(ConnectionMessage.newBuilder()
                .setConnectionId(ConnectionTest.CONNECTION_ID)
                .setMode(ConnectionMessage.ModeType.TERMINATE)
                .build()));
        final byte[] recv = this.managementSocket.read();
        final ConnectionHandshake acknowledgement = (ConnectionHandshake) this.serializer.deserialize(recv);
        Assertions.assertEquals(ConnectionState.TERMINATED, acknowledgement.getConnectionState());
        Assertions.assertEquals("connection-terminated", this.testService.stateQueue.take());
    }

    @AfterEach
    public void closeConnection() throws InterruptedException, IOException {
        ConnectionTest.logger.info("*** END-OF-TEST ***");
        if (this.manager != null) {
            this.manager.close();
            this.manager = null;
        }
        if (this.dataSocket != null) {
            this.dataSocket.close();
            this.dataSocket = null;
        }
    }

    private byte[] readSocketFilterHeartbeat() throws IOException, InterruptedException {
        byte[] data = this.dataSocket.read();

        if (data != null) {
            while (data.length == 1) {
                ConnectionTest.logger.debug("Received heartbeat!");
                data = this.dataSocket.read();
            }
            ConnectionTest.logger.info("Received: {}", new String(data));
        }
        return data;
    }

}
