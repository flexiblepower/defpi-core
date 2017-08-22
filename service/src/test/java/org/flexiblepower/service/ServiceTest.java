/**
 * File this.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.io.Serializable;
import java.net.UnknownHostException;

import org.flexiblepower.exceptions.SerializationException;
import org.flexiblepower.proto.ServiceProto.GoToProcessStateMessage;
import org.flexiblepower.proto.ServiceProto.ProcessState;
import org.flexiblepower.proto.ServiceProto.ProcessStateUpdateMessage;
import org.flexiblepower.proto.ServiceProto.ResumeProcessMessage;
import org.flexiblepower.proto.ServiceProto.SetConfigMessage;
import org.flexiblepower.serializers.JavaIOSerializer;
import org.flexiblepower.serializers.MessageSerializer;
import org.flexiblepower.serializers.ProtobufMessageSerializer;
import org.flexiblepower.service.exceptions.ServiceInvocationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

import com.google.protobuf.ByteString;

/**
 * ServiceTest
 *
 * @author coenvl
 * @version 0.1
 * @since May 12, 2017
 */
public class ServiceTest {

    // private static final String TEST_HOST = "172.17.0.2";
    private static final String TEST_HOST = "localhost";
    private static final String PROCESS_ID = "processID";

    private final TestService testService = new TestService();
    private final MessageSerializer<Serializable> serializer = new JavaIOSerializer();
    private final ProtobufMessageSerializer pbSerializer = new ProtobufMessageSerializer();

    private ServiceManager manager;
    private Socket managementSocket;

    @Before
    public void init() throws InterruptedException, UnknownHostException, ServiceInvocationException {
        this.manager = new ServiceManager(this.testService);

        final String uri = String.format("tcp://%s:%d", ServiceTest.TEST_HOST, ServiceManager.MANAGEMENT_PORT);
        this.managementSocket = ZMQ.context(1).socket(ZMQ.REQ);
        this.managementSocket.setReceiveTimeOut(1000);
        this.managementSocket.connect(uri.toString());

        this.pbSerializer.addMessageClass(GoToProcessStateMessage.class);
        this.pbSerializer.addMessageClass(SetConfigMessage.class);
        this.pbSerializer.addMessageClass(ProcessStateUpdateMessage.class);
        this.pbSerializer.addMessageClass(ResumeProcessMessage.class);
    }

    @Test
    public void runTests() throws SerializationException {
        // One test since they have to be executed in the correct order
        this.runConfigure();
        this.runReconfigure();
        this.runSuspend();
    }

    @Test
    public void runResumeTerminate() throws SerializationException {
        this.runResume();
        this.runTerminate();
    }

    public void runResume() throws SerializationException {
        final byte[] data = this.pbSerializer.serialize(ResumeProcessMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setStateData(ByteString.copyFrom(this.serializer.serialize(TestService.class)))
                .build());
        Assert.assertTrue(this.managementSocket.send(data));

        final byte[] received = this.managementSocket.recv();
        Assert.assertEquals(
                ProcessStateUpdateMessage.newBuilder()
                        .setProcessId(ServiceTest.PROCESS_ID)
                        .setState(ProcessState.RUNNING)
                        .setStateData(ByteString.copyFrom("".getBytes()))
                        .build(),
                this.pbSerializer.deserialize(received));
    }

    public void runConfigure() throws SerializationException {
        final byte[] msg = this.pbSerializer.serialize(SetConfigMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setIsUpdate(false)
                .putConfig("key", "value")
                .build());
        Assert.assertTrue(this.managementSocket.send(msg));
        final byte[] received = this.managementSocket.recv();
        Assert.assertArrayEquals(this.pbSerializer.serialize(ProcessStateUpdateMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setState(ProcessState.RUNNING)
                .setStateData(ByteString.EMPTY)
                .build()), received);
    }

    public void runReconfigure() throws SerializationException {
        Assert.assertTrue(this.managementSocket.send(this.pbSerializer.serialize(SetConfigMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setIsUpdate(true)
                .putConfig("key", "othervalue")
                .build())));
        Assert.assertArrayEquals(this.pbSerializer.serialize(ProcessStateUpdateMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setState(ProcessState.RUNNING)
                .setStateData(ByteString.EMPTY)
                .build()), this.managementSocket.recv());
    }

    // public void runRun() throws SerializationException {
    // Assert.assertTrue(this.managementSocket.send(this.pbSerializer.serialize(GoToProcessStateMessage.newBuilder()
    // .setProcessId(ServiceTest.PROCESS_ID)
    // .setTargetState(ProcessState.RUNNING)
    // .build())));
    // Assert.assertArrayEquals(
    // this.pbSerializer.serialize(
    // ProcessStateUpdateMessage.newBuilder().setProcessId("").setState(ProcessState.RUNNING).build()),
    // this.managementSocket.recv());
    // Assert.assertEquals("init", this.testService.getState());
    // }

    public void runSuspend() throws SerializationException {
        Assert.assertTrue(this.managementSocket.send(this.pbSerializer.serialize(GoToProcessStateMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setTargetState(ProcessState.SUSPENDED)
                .build())));
        final byte[] barr = this.managementSocket.recv();
        Assert.assertArrayEquals(this.pbSerializer.serialize(ProcessStateUpdateMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setState(ProcessState.SUSPENDED)
                .setStateData(ByteString.copyFrom(this.serializer.serialize(TestService.class)))
                .build()), barr);
        Assert.assertEquals("suspend", this.testService.getState());
    }

    public void runTerminate() throws SerializationException {
        Assert.assertTrue(this.managementSocket.send(this.pbSerializer.serialize(GoToProcessStateMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setTargetState(ProcessState.TERMINATED)
                .build())));
        Assert.assertArrayEquals(this.pbSerializer.serialize(ProcessStateUpdateMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setState(ProcessState.TERMINATED)
                .setStateData(ByteString.EMPTY)
                .build()), this.managementSocket.recv());
        Assert.assertEquals("terminate", this.testService.getState());
    }

    @After
    public void stop() throws InterruptedException {
        if (this.manager != null) {
            this.manager.close();
            this.manager = null;
        }
        Thread.sleep(100);
    }

}
