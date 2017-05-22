/**
 * File ServiceTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.io.Serializable;
import java.util.Properties;

import org.flexiblepower.service.proto.ServiceProto.ConnectionMessage;
import org.flexiblepower.service.proto.ServiceProto.GoToProcessStateMessage;
import org.flexiblepower.service.proto.ServiceProto.ProcessState;
import org.flexiblepower.service.serializers.JavaIOSerializer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

/**
 * ServiceTest
 *
 * @author coenvl
 * @version 0.1
 * @since May 12, 2017
 */
@InterfaceInfo(name = "ServiceTest",
               version = "1",
               receiveSerializer = JavaIOSerializer.class,
               receivesHash = "abacac",
               receiveTypes = {Object.class},
               sendSerializer = JavaIOSerializer.class,
               sendsHash = "cacaba",
               sendTypes = {Object.class})
public class ServiceTest implements Service, ConnectionHandlerFactory, ConnectionHandler {

    private static final Logger log = LoggerFactory.getLogger(ServiceTest.class);

    private static ServiceManager manager;

    private static Socket managementSocket;

    @BeforeClass
    public static void init() throws InterruptedException {
        ServiceTest.manager = new ServiceManager(new ServiceTest());

        final ConnectionMessage connection = ConnectionMessage.newBuilder()
                .setConnectionId("1")
                .setMode(ConnectionMessage.ModeType.CREATE)
                .setTargetAddress("tcp://localhost:5025")
                .setListenPort(1234)
                .setReceiveHash("abacac")
                .setSendHash("cacaba")
                .build();

        final String uri = String.format("tcp://%s:%d", "localhost", 4999);

        ServiceTest.managementSocket = ZMQ.context(1).socket(ZMQ.REQ);
        ServiceTest.managementSocket.setReceiveTimeOut(1000);
        ServiceTest.managementSocket.setSendTimeOut(1000);
        ServiceTest.managementSocket.connect(uri.toString());

        Assert.assertTrue(ServiceTest.managementSocket.send(connection.toByteArray()));
        Assert.assertArrayEquals(ServiceManager.SUCCESS, ServiceTest.managementSocket.recv());

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
        /*
         * Assert.assertTrue(ServiceTest.managementSocket.send(ConnectionMessage.newBuilder()
         * .setConnectionId("1")
         * .setMode(ConnectionMessage.ModeType.TERMINATE)
         * .build()
         * .toByteArray()));
         * Assert.assertArrayEquals(ServiceManager.SUCCESS, ServiceTest.managementSocket.recv());
         */

        Assert.assertTrue(ServiceTest.managementSocket.send(GoToProcessStateMessage.newBuilder()
                .setProcessId("Irrelevant")
                .setTargetState(ProcessState.TERMINATED)
                .build()
                .toByteArray()));
        Assert.assertArrayEquals(ServiceManager.SUCCESS, ServiceTest.managementSocket.recv());

        ServiceTest.manager.join();
    }

    public ServiceTest() {
        ConnectionManager.registerHandlers(ServiceTest.class, this);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.Service#resumeFrom(java.lang.Object)
     */
    @Override
    public void resumeFrom(final Serializable state) {
        ServiceTest.log.info("ResumeFrom is called!");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.Service#init(java.util.Properties)
     */
    @Override
    public void init(final Properties props) {
        ServiceTest.log.info("Init is called!");

    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.Service#modify(java.util.Properties)
     */
    @Override
    public void modify(final Properties props) {
        ServiceTest.log.info("Modify is called!");

    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.Service#suspend()
     */
    @Override
    public Serializable suspend() {
        ServiceTest.log.info("Suspend is called!");
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.Service#terminate()
     */
    @Override
    public void terminate() {
        ServiceTest.log.info("Terminate is called!");

    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.ConnectionHandlerFactory#build()
     */
    @Override
    public ConnectionHandler build() {
        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.ConnectionHandler#onConnected(org.flexiblepower.service.Connection)
     */
    @Override
    public void onConnected(final Connection connection) {
        ServiceTest.log.info("onConnect is called!");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.ConnectionHandler#onSuspend()
     */
    @Override
    public void onSuspend() {
        ServiceTest.log.info("onSuspend is called!");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.ConnectionHandler#resumeAfterSuspend()
     */
    @Override
    public void resumeAfterSuspend() {
        ServiceTest.log.info("resumeAfterSuspend is called!");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.ConnectionHandler#onInterrupt()
     */
    @Override
    public void onInterrupt() {
        ServiceTest.log.info("onInterrupt is called!");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.ConnectionHandler#resumeAfterInterrupt()
     */
    @Override
    public void resumeAfterInterrupt() {
        ServiceTest.log.info("resumeAfterInterrupt is called!");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.ConnectionHandler#terminated()
     */
    @Override
    public void terminated() {
        ServiceTest.log.info("terminated is called!");
    }

}
