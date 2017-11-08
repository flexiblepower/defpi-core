/**
 * File MainTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import org.flexiblepower.proto.ConnectionProto.ConnectionState;
import org.flexiblepower.serializers.ProtobufMessageSerializer;
import org.junit.Assert;
import org.junit.Test;

/**
 * MainTest
 *
 * @author coenvl
 * @version 0.1
 * @since Nov 8, 2017
 */
public class MainTest {

    /**
     * MainConnectionHandler
     *
     * @author coenvl
     * @version 0.1
     * @since Nov 8, 2017
     */
    @InterfaceInfo(name = "MainHandler",
                   version = "V001",
                   receivesHash = "123",
                   sendsHash = "321",
                   sendTypes = {},
                   receiveTypes = {},
                   serializer = ProtobufMessageSerializer.class)
    public static class MainConnectionHandler implements ConnectionHandler {

        /**
         * @param c
         * @param s
         */
        public MainConnectionHandler(final Connection c) {
            // TODO Auto-generated constructor stub
        }

        @Override
        public void onSuspend() {
            // TODO Auto-generated method stub

        }

        @Override
        public void resumeAfterSuspend() {
            // TODO Auto-generated method stub

        }

        @Override
        public void onInterrupt() {
            // TODO Auto-generated method stub

        }

        @Override
        public void resumeAfterInterrupt() {
            // TODO Auto-generated method stub

        }

        @Override
        public void terminated() {
            // TODO Auto-generated method stub

        }

    }

    @InterfaceInfo(name = "MainHandler",
                   version = "V002",
                   receivesHash = "1234",
                   sendsHash = "4321",
                   sendTypes = {},
                   receiveTypes = {},
                   serializer = ProtobufMessageSerializer.class)
    public static class MainConnectionHandler2 implements ConnectionHandler {

        /**
         * @param c
         * @param s
         */
        public MainConnectionHandler2(final Connection c) {
            // TODO Auto-generated constructor stub
        }

        @Override
        public void onSuspend() {
            // TODO Auto-generated method stub

        }

        @Override
        public void resumeAfterSuspend() {
            // TODO Auto-generated method stub

        }

        @Override
        public void onInterrupt() {
            // TODO Auto-generated method stub

        }

        @Override
        public void resumeAfterInterrupt() {
            // TODO Auto-generated method stub

        }

        @Override
        public void terminated() {
            // TODO Auto-generated method stub

        }

    }

    private final Connection emptyConnection = new Connection() {

        @Override
        public void send(final Object message) {
            // TODO Auto-generated method stub

        }

        @Override
        public boolean isConnected() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public ConnectionState getState() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String remoteProcessId() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String remoteServiceId() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String remoteInterfaceId() {
            // TODO Auto-generated method stub
            return null;
        }

    };

    public static class TestManager implements ConnectionHandlerManager {

        static int manager_creation_counter = 0;
        static int handler_creation_counter = 0;

        public TestManager() {
            TestManager.manager_creation_counter++;
        }

        public MainConnectionHandler buildV001(final Connection c) {
            TestManager.handler_creation_counter++;
            return new MainConnectionHandler(c);
        }

        public MainConnectionHandler2 buildV002(final Connection c) {
            TestManager.handler_creation_counter++;
            return new MainConnectionHandler2(c);
        }

    }

    @Test // (timeout = 10000)
    public void runServiceMain() {
        ServiceMain.main(new String[] {});
        ConnectionManager.buildHandlerForConnection(this.emptyConnection,
                MainConnectionHandler.class.getAnnotation(InterfaceInfo.class));
        ConnectionManager.buildHandlerForConnection(this.emptyConnection,
                MainConnectionHandler.class.getAnnotation(InterfaceInfo.class));
        ConnectionManager.buildHandlerForConnection(this.emptyConnection,
                MainConnectionHandler2.class.getAnnotation(InterfaceInfo.class));
        Assert.assertEquals(1, TestManager.manager_creation_counter);
        Assert.assertEquals(3, TestManager.handler_creation_counter);
    }

}
