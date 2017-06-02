/**
 * File TestService.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.io.Serializable;
import java.util.Properties;

import org.flexiblepower.service.serializers.JavaIOSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TestService
 *
 * @author coenvl
 * @version 0.1
 * @since May 22, 2017
 */
@InterfaceInfo(name = "Test",
               version = "1",
               serializer = JavaIOSerializer.class,
               receivesHash = "eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252",
               receiveTypes = {Object.class},
               sendsHash = "eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252",
               sendTypes = {Object.class})
public class TestService implements Service, ConnectionHandlerFactory, ConnectionHandler {

    private static final Logger log = LoggerFactory.getLogger(TestService.class);
    private int counter = 0;
    private String state = "";

    public TestService() {
        ConnectionManager.registerHandlers(TestService.class, this);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.Service#resumeFrom(java.lang.Object)
     */
    @Override
    public void resumeFrom(final Serializable state) {
        TestService.log.info("ResumeFrom is called!");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.Service#init(java.util.Properties)
     */
    @Override
    public void init(final Properties props) {
        TestService.log.info("Init is called!");
        this.state = "init";
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.Service#modify(java.util.Properties)
     */
    @Override
    public void modify(final Properties props) {
        TestService.log.info("Modify is called!");
        this.state = "modify";
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.Service#suspend()
     */
    @Override
    public Serializable suspend() {
        TestService.log.info("Suspend is called!");
        this.state = "suspend";
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
        this.state = "terminate";
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
        TestService.log.info("onConnect is called!");
        this.state = "connected";
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.ConnectionHandler#onSuspend()
     */
    @Override
    public void onSuspend() {
        TestService.log.info("onSuspend is called!");
        this.state = "connection-suspended";
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.ConnectionHandler#resumeAfterSuspend()
     */
    @Override
    public void resumeAfterSuspend() {
        TestService.log.info("resumeAfterSuspend is called!");
        this.state = "connection-resumed";
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.ConnectionHandler#onInterrupt()
     */
    @Override
    public void onInterrupt() {
        TestService.log.info("onInterrupt is called!");
        this.state = "connection-interrupted";
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.ConnectionHandler#resumeAfterInterrupt()
     */
    @Override
    public void resumeAfterInterrupt() {
        TestService.log.info("resumeAfterInterrupt is called!");
        this.state = "connection-fixed";
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.ConnectionHandler#terminated()
     */
    @Override
    public void terminated() {
        TestService.log.info("terminated is called!");
        this.state = "connection-terminated";
    }

    public void handleString(final String obj) {
        this.counter++;
        TestService.log.info(" ********** HANDLING {} **************** ", obj);
    }

    public void resetCount() {
        this.counter = 0;
    }

    public int getCounter() {
        return this.counter;
    }

    public String getState() {
        return this.state;
    }

}
