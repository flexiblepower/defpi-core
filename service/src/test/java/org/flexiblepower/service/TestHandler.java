/**
 * File TestHandler.java
 *
 * Copyright 2017 FAN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flexiblepower.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.flexiblepower.proto.ServiceProto.ErrorMessage;
import org.flexiblepower.serializers.ProtobufMessageSerializer;

@SuppressWarnings("javadoc")
@InterfaceInfo(name = "Test",
               version = "1",
               serializer = ProtobufMessageSerializer.class,
               receivesHash = "eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252",
               receiveTypes = {ErrorMessage.class},
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

        public synchronized static TestHandler build1(final Connection c) throws Exception {
            final String name = "h" + ConnectionIntegrationTest.counter++;
            final TestHandler ret = new TestHandler(name, c);
            TestHandler.handlerMap.put(name, ret);
            return ret;
        }
    }

    static Map<String, TestHandler> handlerMap = new HashMap<>();

    static BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    static BlockingQueue<String> stateQueue = new LinkedBlockingQueue<>(2);

    private final Connection connection;
    private final String name;
    public volatile ErrorMessage lastMessage;

    public TestHandler(final String name, final Connection connection) throws Exception {
        this.name = name;

        System.out.println(this.name + ": connected");
        TestHandler.stateQueue.put("connected");
        this.connection = connection;
        connection.send(ErrorMessage.newBuilder().setDebugInformation("started").setProcessId("Error process").build());
    }

    @Override
    public void onSuspend() {
        System.out.println(this.name + ": onSuspend()");
        try {
            TestHandler.stateQueue.put("suspended");
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void resumeAfterSuspend() {
        System.out.println(this.name + ": resumeAfterSuspend()");
        try {
            TestHandler.stateQueue.put("resume-suspended");
        } catch (final InterruptedException e1) {
            e1.printStackTrace();
        }
        try {
            this.connection.send(ErrorMessage.newBuilder()
                    .setDebugInformation("resumed from suspend")
                    .setProcessId("Error process")
                    .build());
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onInterrupt() {
        System.out.println(this.name + ": onInterrupt()");
        try {
            TestHandler.stateQueue.put("interrupted");
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void resumeAfterInterrupt() {
        System.out.println(this.name + ": resumeAfterInterrupt()");
        try {
            TestHandler.stateQueue.put("resume-interrupted");
        } catch (final InterruptedException e1) {
            e1.printStackTrace();
        }
        try {
            this.connection.send(ErrorMessage.newBuilder()
                    .setDebugInformation("resumed from interrupt")
                    .setProcessId("Error process")
                    .build());
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void terminated() {
        System.out.println(this.name + ": terminated()");
        try {
            TestHandler.stateQueue.put("terminated");
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void handleErrorMessageMessage(final ErrorMessage o) {
        System.out.println(this.name + ": received object with value " + o.getDebugInformation());
        this.lastMessage = o;
        TestHandler.messageQueue.add(o.getDebugInformation());
    }

}