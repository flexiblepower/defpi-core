/**
 * File ConnectionTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flexiblepower.proto.ServiceProto.ErrorMessage;
import org.flexiblepower.serializers.ProtobufMessageSerializer;
import org.flexiblepower.service.exceptions.ConnectionModificationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

// These tests are useful, but take very long
@RunWith(Parameterized.class)
public class ConnectionIntegrationTest {

    protected static Map<String, TestHandler> handlerMap = new HashMap<>();
    protected static int counter;

    @Parameters
    public static List<Object[]> data() {
        return Arrays.asList(new Object[3][0]);
    }

    /**
     * TestHandlerBuilder
     *
     * @author coenvl
     * @version 0.1
     * @since Aug 22, 2017
     */
    public class TestHandlerBuilder implements ConnectionHandlerManager {

        public synchronized TestHandler build1(final Connection c) {
            final String name = "h" + ConnectionIntegrationTest.counter++;
            final TestHandler ret = new TestHandler(name, c);
            ConnectionIntegrationTest.handlerMap.put(name, ret);
            return ret;
        }
    }

    @InterfaceInfo(name = "Test",
                   version = "1",
                   serializer = ProtobufMessageSerializer.class,
                   receivesHash = "eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252",
                   receiveTypes = {ErrorMessage.class},
                   manager = TestService.class,
                   sendsHash = "eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252",
                   sendTypes = {ErrorMessage.class})
    public static class TestHandler implements ConnectionHandler {

        private final String name;
        public ErrorMessage lastMessage;
        public String state;

        public TestHandler(final String name, final Connection connection) {
            this.name = name;

            System.out.println(this.name + ": connected");
            this.state = "connected";
            if (this.name.equals("h1")) {
                connection.send(
                        ErrorMessage.newBuilder().setDebugInformation("test").setProcessId("Error process").build());
            }
        }

        @Override
        public void onSuspend() {
            System.out.println(this.name + ": onSuspend()");
            this.state = "suspended";
        }

        @Override
        public void resumeAfterSuspend() {
            System.out.println(this.name + ": resumeAfterSuspend()");
            this.state = "resume-suspended";
        }

        @Override
        public void onInterrupt() {
            System.out.println(this.name + ": onInterrupt()");
            this.state = "interrupted";
        }

        @Override
        public void resumeAfterInterrupt() {
            System.out.println(this.name + ": resumeAfterInterrupt()");
            this.state = "resume-interrupted";
        }

        @Override
        public void terminated() {
            System.out.println(this.name + ": terminated()");
            this.state = "terminated";
        }

        public void handleErrorMessageMessage(final ErrorMessage o) {
            System.out.println(this.name + ": received object with value " + o.getDebugInformation());
            this.lastMessage = o;
        }

    }

    // @Test
    // public void runTests() throws Exception {
    // for (int i = 0; i < 100; i++) {
    // this.reset();
    // this.testNormalConnection();
    // this.reset();
    // this.testInterruptDetectionAndResume();
    // this.reset();
    // this.testSuspendAndResume();
    // }
    // }

    @Before
    @SuppressWarnings("static-method")
    public void reset() throws InterruptedException {
        ConnectionIntegrationTest.handlerMap.clear();
        ConnectionIntegrationTest.counter = 1;
        Thread.sleep(100);
        ServiceExecutor.getInstance().shutDown();
    }

    @Test(timeout = 10000)
    public void testNormalConnection() throws ConnectionModificationException, InterruptedException {

        final InterfaceInfo info = TestHandler.class.getAnnotation(InterfaceInfo.class);
        ConnectionManager.registerConnectionHandlerFactory(TestHandler.class, new TestHandlerBuilder());

        try (
                final ManagedConnection mc1 = new ManagedConnection("CIT", 5000, "tcp://localhost:5001", info);
                final ManagedConnection mc2 = new ManagedConnection("CIT", 5001, "tcp://localhost:5000", info)) {

            Thread.sleep(1500); // Make sure at least 1 heartbeat is sent

            Assert.assertEquals("connected", ConnectionIntegrationTest.handlerMap.get("h1").state);
            Assert.assertEquals("connected", ConnectionIntegrationTest.handlerMap.get("h2").state);

            Assert.assertNull(ConnectionIntegrationTest.handlerMap.get("h1").lastMessage);
            Assert.assertNotNull(ConnectionIntegrationTest.handlerMap.get("h2").lastMessage);

            mc1.goToTerminatedState();
            mc2.goToTerminatedState();

            Thread.sleep(500);

            Assert.assertEquals("terminated", ConnectionIntegrationTest.handlerMap.get("h1").state);
            Assert.assertEquals("terminated", ConnectionIntegrationTest.handlerMap.get("h2").state);
        }
    }

    @Test(timeout = 30000)
    public void testInterruptDetectionAndResume() throws ConnectionModificationException, InterruptedException {
        final InterfaceInfo info = TestHandler.class.getAnnotation(InterfaceInfo.class);
        ConnectionManager.registerConnectionHandlerFactory(TestHandler.class, new TestHandlerBuilder());

        try (final ManagedConnection mc2 = new ManagedConnection("CIT", 5001, "tcp://localhost:5000", info)) {
            try (final ManagedConnection mc1 = new ManagedConnection("CIT", 5000, "tcp://localhost:5001", info)) {
                Thread.sleep(1500);

                Assert.assertEquals("connected", ConnectionIntegrationTest.handlerMap.get("h1").state);
                Assert.assertEquals("connected", ConnectionIntegrationTest.handlerMap.get("h2").state);

                Thread.sleep(500);
                // By now we must have slept longer than HeartBeatMonitor.HEARTBEAT_INITIAL_DELAY

                // Close ONE. However, we cannot predict which handler is attached to it
                mc1.goToTerminatedState();
            }

            Thread.sleep(12000);

            String state1 = ConnectionIntegrationTest.handlerMap.get("h1").state;
            String state2 = ConnectionIntegrationTest.handlerMap.get("h2").state;
            System.out.println("State 1 : " + state1);
            System.out.println("State 2 : " + state2);

            Assert.assertTrue(("terminated".equals(state1) && "interrupted".equals(state2))
                    || ("terminated".equals(state2) && "interrupted".equals(state1)));

            try (final ManagedConnection mc3 = new ManagedConnection("CIT", 5000, "tcp://localhost:5001", info)) {
                Thread.sleep(1000); // Timing is important here since there is an exponential backoff

                state1 = ConnectionIntegrationTest.handlerMap.get("h1").state;
                state2 = ConnectionIntegrationTest.handlerMap.get("h2").state;
                final String state3 = ConnectionIntegrationTest.handlerMap.get("h3").state;
                System.out.println("State 1 : " + state1);
                System.out.println("State 2 : " + state2);
                System.out.println("State 3 : " + state3);

                Assert.assertTrue(("connected".equals(state3) && "resume-interrupted".equals(state2))
                        || ("connected".equals(state3) && "resume-interrupted".equals(state1)));
            }

            // Assert.assertEquals("connected", ConnectionIntegrationTest.handlerMap.get("h1").state);
            // Assert.assertEquals("resume-interrupted", ConnectionIntegrationTest.handlerMap.get("h2").state);
        }
    }

    @Test(timeout = 15000)
    public void testSuspendAndResume() throws ConnectionModificationException, InterruptedException {
        final InterfaceInfo info = TestHandler.class.getAnnotation(InterfaceInfo.class);
        ConnectionManager.registerConnectionHandlerFactory(TestHandler.class, new TestHandlerBuilder());

        try (
                final ManagedConnection mc1 = new ManagedConnection("CIT", 5000, "tcp://localhost:5001", info);
                final ManagedConnection mc2 = new ManagedConnection("CIT", 5001, "tcp://localhost:5000", info)) {

            Thread.sleep(1500);

            Assert.assertEquals("connected", ConnectionIntegrationTest.handlerMap.get("h1").state);
            Assert.assertEquals("connected", ConnectionIntegrationTest.handlerMap.get("h2").state);

            mc1.goToSuspendedState();
            mc2.goToSuspendedState();

            Thread.sleep(500);

            Assert.assertEquals("suspended", ConnectionIntegrationTest.handlerMap.get("h1").state);
            Assert.assertEquals("suspended", ConnectionIntegrationTest.handlerMap.get("h2").state);

            // Now we move mc2 to port 5002
            mc1.goToResumedState(5000, "tcp://localhost:5002");
            mc2.goToResumedState(5002, "tcp://localhost:5000");

            Thread.sleep(500);

            Assert.assertEquals("resume-suspended", ConnectionIntegrationTest.handlerMap.get("h1").state);
            Assert.assertEquals("resume-suspended", ConnectionIntegrationTest.handlerMap.get("h2").state);
        }
    }
}
