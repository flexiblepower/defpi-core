/**
 * File this.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.io.Serializable;
import java.net.UnknownHostException;

import org.flexiblepower.service.exceptions.SerializationException;
import org.flexiblepower.service.proto.ServiceProto.GoToProcessStateMessage;
import org.flexiblepower.service.proto.ServiceProto.ProcessState;
import org.flexiblepower.service.proto.ServiceProto.ResumeProcessMessage;
import org.flexiblepower.service.serializers.JavaIOSerializer;
import org.flexiblepower.service.serializers.MessageSerializer;
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

    private final TestService testService = new TestService();
    private final MessageSerializer<Serializable> serializer = new JavaIOSerializer();
    private ServiceManager manager;
    private Socket managementSocket;

    @Before
    public void init() throws InterruptedException, UnknownHostException {
        this.manager = new ServiceManager(this.testService);

        final String uri = String.format("tcp://%s:%d", ServiceTest.TEST_HOST, ServiceManager.MANAGEMENT_PORT);
        this.managementSocket = ZMQ.context(1).socket(ZMQ.REQ);
        this.managementSocket.setReceiveTimeOut(1000);
        this.managementSocket.connect(uri.toString());
    }

    @Test(timeout = 5000)
    public void runResume() throws SerializationException {
        Assert.assertTrue(this.managementSocket.send(ResumeProcessMessage.newBuilder()
                .setProcessId("NotImportant")
                .setStateData(ByteString.copyFrom(this.serializer.serialize(TestService.class)))
                .build()
                .toByteArray()));
        Assert.assertArrayEquals(ServiceManager.SUCCESS, this.managementSocket.recv());
    }

    @Test(timeout = 5000)
    public void runStart() {
        Assert.assertTrue(this.managementSocket.send(GoToProcessStateMessage.newBuilder()
                .setProcessId("haha")
                .setTargetState(ProcessState.STARTING)
                .build()
                .toByteArray()));
        Assert.assertArrayEquals(ServiceManager.FAILURE, this.managementSocket.recv());
    }

    @Test(timeout = 5000)
    public void runInit() {
        Assert.assertTrue(this.managementSocket.send(GoToProcessStateMessage.newBuilder()
                .setProcessId("")
                .setTargetState(ProcessState.INITIALIZING)
                .build()
                .toByteArray()));
        Assert.assertArrayEquals(ServiceManager.FAILURE, this.managementSocket.recv());
    }

    @Test(timeout = 5000)
    public void runRun() {
        Assert.assertTrue(this.managementSocket.send(GoToProcessStateMessage.newBuilder()
                .setProcessId("")
                .setTargetState(ProcessState.RUNNING)
                .build()
                .toByteArray()));
        Assert.assertArrayEquals(ServiceManager.SUCCESS, this.managementSocket.recv());
        Assert.assertEquals("init", this.testService.getState());
    }

    @Test(timeout = 5000)
    public void runSuspend() throws SerializationException {
        Assert.assertTrue(this.managementSocket.send(GoToProcessStateMessage.newBuilder()
                .setProcessId("")
                .setTargetState(ProcessState.SUSPENDED)
                .build()
                .toByteArray()));
        final byte[] barr = this.managementSocket.recv();
        Assert.assertEquals(TestService.class, this.serializer.deserialize(barr));
        Assert.assertEquals("suspend", this.testService.getState());
    }

    @Test(timeout = 5000)
    public void runTerminate() {
        Assert.assertTrue(this.managementSocket.send(GoToProcessStateMessage.newBuilder()
                .setProcessId("Irrelevant")
                .setTargetState(ProcessState.TERMINATED)
                .build()
                .toByteArray()));
        Assert.assertArrayEquals(ServiceManager.SUCCESS, this.managementSocket.recv());
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
