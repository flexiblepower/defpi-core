/**
 * File TestService.java
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

import java.io.Serializable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.flexiblepower.serializers.JavaIOSerializer;
import org.flexiblepower.service.TestService.TestServiceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TestService
 *
 * @version 0.1
 * @since May 22, 2017
 */
@SuppressWarnings("javadoc")
@InterfaceInfo(name = "Test",
               version = "1",
               serializer = JavaIOSerializer.class,
               receivesHash = "eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252",
               receiveTypes = {Object.class},
               sendsHash = "eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252",
               sendTypes = {Object.class})
public class TestService implements Service<TestServiceConfiguration>, ConnectionHandlerManager, ConnectionHandler {

    /**
     * TestServiceConfiguration
     *
     * @version 0.1
     * @since Aug 28, 2017
     */
    public interface TestServiceConfiguration {

        public String getKey();

        public boolean getMakeMeThrowAnError();

    }

    private static final Logger log = LoggerFactory.getLogger(TestService.class);
    private int counter = 0;

    final BlockingQueue<String> stateQueue = new LinkedBlockingQueue<>();

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.Service#resumeFrom(java.lang.Object)
     */
    @Override
    public void resumeFrom(final Serializable resumeState) {
        TestService.log.info("ResumeFrom is called!");
        this.stateQueue.add("resumed");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.Service#init(java.util.Properties)
     */
    @Override
    public void init(final TestServiceConfiguration props, final DefPiParameters params) {
        TestService.log.info("Init is called with key {}!", props.getKey());
        this.stateQueue.add("init");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.Service#modify(java.util.Properties)
     */
    @Override
    public void modify(final TestServiceConfiguration props) {
        TestService.log.info("Modify is called with key {}!", props.getKey());
        if (props.getMakeMeThrowAnError()) {
            throw new RuntimeException("I am an error!");
        }
        this.stateQueue.add("modify");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.Service#suspend()
     */
    @Override
    public Serializable suspend() {
        TestService.log.info("Suspend is called!");
        this.stateQueue.add("suspend");
        return this.getClass();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.Service#terminate()
     */
    @Override
    public void terminate() {
        TestService.log.info("Terminate is called!");
        this.stateQueue.add("terminate");
    }

    /**
     * This is the function the ConnectionManager will look for to build the handler
     *
     * @param connection The connection object that requires its messages to be handled.
     * @return the TestService itself
     */
    public ConnectionHandler build1(final Connection connection) {
        TestService.log.info("build is called!");
        this.stateQueue.add("connected");
        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.ConnectionHandler#onSuspend()
     */
    @Override
    public void onSuspend() {
        TestService.log.info("onSuspend is called!");
        this.stateQueue.add("connection-suspended");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.ConnectionHandler#resumeAfterSuspend()
     */
    @Override
    public void resumeAfterSuspend() {
        TestService.log.info("resumeAfterSuspend is called!");
        this.stateQueue.add("connection-resumed");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.ConnectionHandler#onInterrupt()
     */
    @Override
    public void onInterrupt() {
        TestService.log.info("onInterrupt is called!");
        this.stateQueue.add("connection-interrupted");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.ConnectionHandler#resumeAfterInterrupt()
     */
    @Override
    public void resumeAfterInterrupt() {
        TestService.log.info("resumeAfterInterrupt is called!");
        this.stateQueue.add("connection-fixed");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.ConnectionHandler#terminated()
     */
    @Override
    public void terminated() {
        TestService.log.info("terminated is called!");
        this.stateQueue.add("connection-terminated");
    }

    public void handleStringMessage(final String obj) {
        this.counter--;
        if (this.counter == 0) {
            this.stateQueue.add("Received all messages!");
        }
        TestService.log.debug(" ********** HANDLING {} **************** ", obj);
    }

    public int getCounter() {
        return this.counter;
    }

    public void expect(final int numTests) {
        this.counter = numTests;
    }

}
