/**
 * File TestHandler.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.util.HashMap;
import java.util.Map;

import org.flexiblepower.proto.ServiceProto.ErrorMessage;
import org.flexiblepower.serializers.ProtobufMessageSerializer;

@InterfaceInfo(name = "Test",
               version = "1",
               serializer = ProtobufMessageSerializer.class,
               receivesHash = "eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252",
               receiveTypes = {ErrorMessage.class},
               // manager = TestService.class,
               sendsHash = "eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252",
               sendTypes = {ErrorMessage.class})
public class TestHandler implements ConnectionHandler {

    /**
     * TestHandlerBuilder
     *
     * @author coenvl
     * @version 0.1
     * @since Aug 22, 2017
     */
    public static class TestHandlerBuilder implements ConnectionHandlerManager {

        public synchronized static TestHandler build1(final Connection c) throws InterruptedException {
            final String name = "h" + ConnectionIntegrationTest.counter++;
            final TestHandler ret = new TestHandler(name, c);
            TestHandler.handlerMap.put(name, ret);
            return ret;
        }
    }

    static Map<String, TestHandler> handlerMap = new HashMap<>();

    private final Connection connection;
    private final String name;
    public ErrorMessage lastMessage;
    public String state;

    public TestHandler(final String name, final Connection connection) throws InterruptedException {
        this.name = name;

        System.out.println(this.name + ": connected");
        this.state = "connected";
        this.connection = connection;
        if (this.name.equals("h1")) {
            // Thread.sleep(100);
            connection.send(
                    ErrorMessage.newBuilder().setDebugInformation("started").setProcessId("Error process").build());
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
        if (this.name.equals("h1")) {
            this.connection.send(ErrorMessage.newBuilder()
                    .setDebugInformation("resumed from suspend")
                    .setProcessId("Error process")
                    .build());
        }
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
        this.connection.send(ErrorMessage.newBuilder()
                .setDebugInformation("resumed from interrupt")
                .setProcessId("Error process")
                .build());
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