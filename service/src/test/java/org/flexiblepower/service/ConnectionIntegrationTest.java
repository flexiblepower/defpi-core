/**
 * File ConnectionTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.flexiblepower.proto.ConnectionProto.ConnectionHandshake;
import org.flexiblepower.proto.ConnectionProto.ConnectionState;
import org.flexiblepower.serializers.ProtobufMessageSerializer;
import org.flexiblepower.service.exceptions.ConnectionModificationException;
import org.junit.Assert;
import org.junit.Test;

// @Ignore // These tests are usefull, but take very long
public class ConnectionIntegrationTest {

    @InterfaceInfo(name = "Test",
                   version = "1",
                   serializer = ProtobufMessageSerializer.class,
                   receivesHash = "eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252",
                   receiveTypes = {ConnectionHandshake.class},
                   manager = TestService.class,
                   sendsHash = "eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252",
                   sendTypes = {ConnectionHandshake.class})
    public static class TestHandler implements ConnectionHandler {

        private final String name;
        private final Connection connection;
        public ConnectionHandshake lastMessage;
        public String state;

        public TestHandler(final String name, final Connection connection) {
            this.name = name;

            System.out.println(this.name + ": connected");
            this.state = "connected";
            this.connection = connection;
            if (this.name.equals("h1")) {
                connection.send(ConnectionHandshake.newBuilder()
                        .setConnectionId("test")
                        .setConnectionState(ConnectionState.CONNECTED)
                        .build());
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

        public void handleConnectionHandshakeMessage(final ConnectionHandshake o) {
            System.out.println(this.name + ": received object with value " + o.getConnectionId());
            this.lastMessage = o;
        }

    }

    final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2);

    @Test
    public void testNormalConnection() throws ConnectionModificationException, InterruptedException {

        final InterfaceInfo info = TestHandler.class.getAnnotation(InterfaceInfo.class);

        final ManagedConnection mc1 = new ManagedConnection("connectionId", 5000, "tcp://localhost:5001", info);
        final ManagedConnection mc2 = new ManagedConnection("connectionId", 5001, "tcp://localhost:5000", info);

        final TestHandler h1 = new TestHandler("h1", mc1);
        final TestHandler h2 = new TestHandler("h2", mc2);

        Thread.sleep(1000);

        Assert.assertEquals("connected", h1.state);
        Assert.assertEquals("connected", h2.state);

        Assert.assertNull(h1.lastMessage);
        Assert.assertNotNull(h2.lastMessage);

        mc1.goToTerminatedState();
        mc2.goToTerminatedState();

        Thread.sleep(1000);

        Assert.assertEquals("terminated", h1.state);
        Assert.assertEquals("terminated", h2.state);

        Thread.sleep(3000);
    }

    @Test
    public void testInterruptDetectionAndResume() throws ConnectionModificationException, InterruptedException {
        final InterfaceInfo info = TestHandler.class.getAnnotation(InterfaceInfo.class);

        ManagedConnection mc1 = new ManagedConnection("connectionId", 5000, "tcp://localhost:5001", info);
        final ManagedConnection mc2 = new ManagedConnection("connectionId", 5001, "tcp://localhost:5000", info);

        final TestHandler h1 = new TestHandler("h1", mc1);
        final TestHandler h2 = new TestHandler("h2", mc2);

        Thread.sleep(1000);

        Assert.assertEquals("connected", h1.state);
        Assert.assertEquals("connected", h2.state);

        Thread.sleep(1000);

        mc1.goToTerminatedState();

        Thread.sleep(12000);

        Assert.assertEquals("interrupted", h2.state);

        mc1 = new ManagedConnection("connectionId", 5000, "tcp://localhost:5001", info);

        Thread.sleep(10000); // Timing is important here since there is an exponential backoff

        Assert.assertEquals("connected", h1.state);
        Assert.assertEquals("resume-interrupted", h2.state);

        mc1.goToTerminatedState();
        mc2.goToTerminatedState();
        Thread.sleep(3000);
    }

    @Test
    public void testSuspendAndResume() throws ConnectionModificationException, InterruptedException {
        final InterfaceInfo info = TestHandler.class.getAnnotation(InterfaceInfo.class);

        final ManagedConnection mc1 = new ManagedConnection("connectionId", 5000, "tcp://localhost:5001", info);
        final ManagedConnection mc2 = new ManagedConnection("connectionId", 5001, "tcp://localhost:5000", info);

        final TestHandler h1 = new TestHandler("h1", mc1);
        final TestHandler h2 = new TestHandler("h2", mc2);

        Thread.sleep(1000);

        Assert.assertEquals("connected", h1.state);
        Assert.assertEquals("connected", h2.state);

        mc1.goToSuspendedState();
        mc2.goToSuspendedState();

        Thread.sleep(1000);

        Assert.assertEquals("suspended", h1.state);
        Assert.assertEquals("suspended", h2.state);

        // Now we move mc2 to port 5002
        mc1.resumeAfterSuspendedState(5000, "tcp://localhost:5002");
        mc2.resumeAfterSuspendedState(5002, "tcp://localhost:5000");

        Thread.sleep(5000);

        Assert.assertEquals("resume-suspended", h2.state);
        Assert.assertEquals("resume-suspended", h2.state);

        mc1.goToTerminatedState();
        mc2.goToTerminatedState();
        Thread.sleep(3000);
    }
}
