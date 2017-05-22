/**
 * File ServiceTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import org.flexiblepower.service.exceptions.SerializationException;
import org.flexiblepower.service.proto.ServiceProto.ConnectionMessage;
import org.flexiblepower.service.proto.ServiceProto.GoToProcessStateMessage;
import org.flexiblepower.service.proto.ServiceProto.ProcessState;
import org.flexiblepower.service.serializers.JavaIOSerializer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

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

    private static ServiceManager manager;

    private static Socket managementSocket;

    @BeforeClass
    public static void init() throws InterruptedException {
        ServiceTest.manager = new ServiceManager(new TestService());

        final String uri = String.format("tcp://%s:%d", ServiceTest.TEST_HOST, 4999);
        ServiceTest.managementSocket = ZMQ.context(1).socket(ZMQ.REQ);
        ServiceTest.managementSocket.setReceiveTimeOut(1000);
        ServiceTest.managementSocket.setSendTimeOut(1000);
        ServiceTest.managementSocket.connect(uri.toString());

        final ConnectionMessage connection = ConnectionMessage.newBuilder()
                .setConnectionId("1")
                .setMode(ConnectionMessage.ModeType.CREATE)
                .setTargetAddress("tcp://localhost:5025")
                .setListenPort(1234)
                .setReceiveHash("eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252")
                .setSendHash("eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252")
                .build();

        Assert.assertTrue(ServiceTest.managementSocket.send(connection.toByteArray()));
        Assert.assertArrayEquals(ServiceManager.SUCCESS, ServiceTest.managementSocket.recv());
    }

    @Test
    public void testSend() throws InterruptedException, SerializationException {
        final String uri = String.format("tcp://%s:%d", ServiceTest.TEST_HOST, 1234);
        final Socket testSocket = ZMQ.context(2).socket(ZMQ.PUSH);
        // testSocket.setReceiveTimeOut(1000);
        testSocket.setSendTimeOut(1000);
        testSocket.connect(uri.toString());

        for (int i = 0; i <= 100; i++) {
            Assert.assertTrue(testSocket.send((new JavaIOSerializer()).serialize("THIS IS A TEST " + i)));
        }
        Thread.sleep(100);
    }

    @Test
    public void runStart() {
        Assert.assertTrue(ServiceTest.managementSocket.send(GoToProcessStateMessage.newBuilder()
                .setProcessId("haha")
                .setTargetState(ProcessState.STARTING)
                .build()
                .toByteArray()));
        Assert.assertArrayEquals(ServiceManager.FAILURE, ServiceTest.managementSocket.recv());
    }

    @Test
    public void runInit() {
        Assert.assertTrue(ServiceTest.managementSocket.send(GoToProcessStateMessage.newBuilder()
                .setProcessId("")
                .setTargetState(ProcessState.INITIALIZING)
                .build()
                .toByteArray()));
        Assert.assertArrayEquals(ServiceManager.FAILURE, ServiceTest.managementSocket.recv());
    }

    @Test
    public void runRun() {
        Assert.assertTrue(ServiceTest.managementSocket.send(GoToProcessStateMessage.newBuilder()
                .setProcessId("")
                .setTargetState(ProcessState.RUNNING)
                .build()
                .toByteArray()));
        Assert.assertArrayEquals(ServiceManager.SUCCESS, ServiceTest.managementSocket.recv());
    }

    @Test
    public void runSuspend() {
        Assert.assertTrue(ServiceTest.managementSocket.send(GoToProcessStateMessage.newBuilder()
                .setProcessId("")
                .setTargetState(ProcessState.SUSPENDED)
                .build()
                .toByteArray()));
        Assert.assertArrayEquals(ServiceManager.SUCCESS, ServiceTest.managementSocket.recv());
    }

    @AfterClass
    public static void stop() throws InterruptedException {
        Assert.assertTrue(ServiceTest.managementSocket.send(ConnectionMessage.newBuilder()
                .setConnectionId("1")
                .setMode(ConnectionMessage.ModeType.TERMINATE)
                .build()
                .toByteArray()));
        Assert.assertArrayEquals(ServiceManager.SUCCESS, ServiceTest.managementSocket.recv());

        Assert.assertTrue(ServiceTest.managementSocket.send(GoToProcessStateMessage.newBuilder()
                .setProcessId("Irrelevant")
                .setTargetState(ProcessState.TERMINATED)
                .build()
                .toByteArray()));
        Assert.assertArrayEquals(ServiceManager.SUCCESS, ServiceTest.managementSocket.recv());

        ServiceTest.manager.join();
    }

}
