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

    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.Service#modify(java.util.Properties)
     */
    @Override
    public void modify(final Properties props) {
        TestService.log.info("Modify is called!");

    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.Service#suspend()
     */
    @Override
    public Serializable suspend() {
        TestService.log.info("Suspend is called!");
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.Service#terminate()
     */
    @Override
    public void terminate() {
        TestService.log.info("Terminate is called!");

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
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.ConnectionHandler#onSuspend()
     */
    @Override
    public void onSuspend() {
        TestService.log.info("onSuspend is called!");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.ConnectionHandler#resumeAfterSuspend()
     */
    @Override
    public void resumeAfterSuspend() {
        TestService.log.info("resumeAfterSuspend is called!");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.ConnectionHandler#onInterrupt()
     */
    @Override
    public void onInterrupt() {
        TestService.log.info("onInterrupt is called!");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.ConnectionHandler#resumeAfterInterrupt()
     */
    @Override
    public void resumeAfterInterrupt() {
        TestService.log.info("resumeAfterInterrupt is called!");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.ConnectionHandler#terminated()
     */
    @Override
    public void terminated() {
        TestService.log.info("terminated is called!");
    }

    public void handleString(final String obj) {
        TestService.log.info(" ********** HANDLING {} **************** ", obj);
    }

}
