/**
 * File this.java
 *
 * Copyright 2017 FAN
 */
package org.flexiblepower.service;

import java.io.Serializable;

import org.flexiblepower.commons.TCPSocket;
import org.flexiblepower.proto.ServiceProto.ErrorMessage;
import org.flexiblepower.proto.ServiceProto.GoToProcessStateMessage;
import org.flexiblepower.proto.ServiceProto.ProcessState;
import org.flexiblepower.proto.ServiceProto.ProcessStateUpdateMessage;
import org.flexiblepower.proto.ServiceProto.ResumeProcessMessage;
import org.flexiblepower.proto.ServiceProto.SetConfigMessage;
import org.flexiblepower.serializers.JavaIOSerializer;
import org.flexiblepower.serializers.MessageSerializer;
import org.flexiblepower.serializers.ProtobufMessageSerializer;
import org.flexiblepower.service.TestService.TestServiceConfiguration;
import org.flexiblepower.service.exceptions.ServiceInvocationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.protobuf.ByteString;

/**
 * ServiceTest
 *
 * @version 0.1
 * @since May 12, 2017
 */
public class ServiceTest {

    // private static final String TEST_HOST = "172.17.0.2";
    private static final String TEST_HOST = "localhost";
    private static final String PROCESS_ID = "null";

    private final TestService testService = new TestService();
    private final MessageSerializer<Serializable> serializer = new JavaIOSerializer();
    private final ProtobufMessageSerializer pbSerializer = new ProtobufMessageSerializer();

    private ServiceManager<TestServiceConfiguration> manager;
    private TCPSocket managementSocket;

    @Before
    public void init() throws Exception {
        this.manager = new ServiceManager<>();
        try {
            this.manager.start(this.testService);
        } catch (final ServiceInvocationException e) {
            Assert.assertTrue(e.getMessage().startsWith("Futile"));
        }

        this.managementSocket = TCPSocket.asClient(ServiceTest.TEST_HOST, ServiceManager.MANAGEMENT_PORT);
        this.managementSocket.waitUntilConnected(0);

        this.pbSerializer.addMessageClass(GoToProcessStateMessage.class);
        this.pbSerializer.addMessageClass(SetConfigMessage.class);
        this.pbSerializer.addMessageClass(ProcessStateUpdateMessage.class);
        this.pbSerializer.addMessageClass(ResumeProcessMessage.class);
        this.pbSerializer.addMessageClass(ErrorMessage.class);
    }

    @Test(timeout = 60000)
    public void runTests() throws Exception {
        // One test since they have to be executed in the correct order
        this.runConfigure();
        this.runReconfigure();
        this.runWithError();
        this.runSuspend();
    }

    @Test(timeout = 60000)
    public void runResumeTerminate() throws Exception {
        this.runResume();
        this.runTerminate();
    }

    public void runResume() throws Exception {
        final byte[] data = this.pbSerializer.serialize(ResumeProcessMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setStateData(ByteString.copyFrom(this.serializer.serialize(TestService.class)))
                .build());
        this.managementSocket.send(data);

        final byte[] received = this.managementSocket.read();
        Assert.assertEquals(
                ProcessStateUpdateMessage.newBuilder()
                        .setProcessId(ServiceTest.PROCESS_ID)
                        .setState(ProcessState.RUNNING)
                        .setStateData(ByteString.copyFrom("".getBytes()))
                        .build(),
                this.pbSerializer.deserialize(received));
        Thread.sleep(100);
        Assert.assertEquals("resumed", this.testService.getState());
    }

    public void runConfigure() throws Exception {
        final byte[] msg = this.pbSerializer.serialize(SetConfigMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setIsUpdate(false)
                .putConfig("key", "value")
                .build());
        this.managementSocket.send(msg);
        final byte[] received = this.managementSocket.read();
        Assert.assertArrayEquals(this.pbSerializer.serialize(ProcessStateUpdateMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setState(ProcessState.RUNNING)
                .setStateData(ByteString.EMPTY)
                .build()), received);
        Thread.sleep(100);
        Assert.assertEquals("init", this.testService.getState());
    }

    public void runWithError() throws Exception {
        final byte[] msg = this.pbSerializer.serialize(SetConfigMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setIsUpdate(false)
                .putConfig("makeMeThrowAnError", "true")
                .build());
        this.managementSocket.send(msg);
        final byte[] received = this.managementSocket.read();
        final Object err = this.pbSerializer.deserialize(received);
        Assert.assertEquals(ErrorMessage.class, err.getClass());
        Assert.assertEquals(ServiceTest.PROCESS_ID, ((ErrorMessage) err).getProcessId());
    }

    public void runReconfigure() throws Exception {
        this.managementSocket.send(this.pbSerializer.serialize(SetConfigMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setIsUpdate(true)
                .putConfig("key", "othervalue")
                .build()));
        Assert.assertArrayEquals(this.pbSerializer.serialize(ProcessStateUpdateMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setState(ProcessState.RUNNING)
                .setStateData(ByteString.EMPTY)
                .build()), this.managementSocket.read());
        Thread.sleep(100);
        Assert.assertEquals("modify", this.testService.getState());
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

    public void runSuspend() throws Exception {
        this.managementSocket.send(this.pbSerializer.serialize(GoToProcessStateMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setTargetState(ProcessState.SUSPENDED)
                .build()));
        final byte[] barr = this.managementSocket.read();
        Assert.assertArrayEquals(this.pbSerializer.serialize(ProcessStateUpdateMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setState(ProcessState.SUSPENDED)
                .setStateData(ByteString.copyFrom(this.serializer.serialize(TestService.class)))
                .build()), barr);
        Thread.sleep(100);
        Assert.assertEquals("suspend", this.testService.getState());
    }

    public void runTerminate() throws Exception {
        this.managementSocket.send(this.pbSerializer.serialize(GoToProcessStateMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setTargetState(ProcessState.TERMINATED)
                .build()));
        Assert.assertArrayEquals(this.pbSerializer.serialize(ProcessStateUpdateMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setState(ProcessState.TERMINATED)
                .setStateData(ByteString.EMPTY)
                .build()), this.managementSocket.read());
        Thread.sleep(100);
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
