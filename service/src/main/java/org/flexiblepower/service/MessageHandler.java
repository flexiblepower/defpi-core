/**
 * File MessageHandler.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

/**
 * MessageHandler
 *
 * @author coenvl
 * @version 0.1
 * @since Apr 24, 2017
 */
public interface MessageHandler {

    void onConnected(Connection connection);

    void onSuspend();

    void resumeAfterSuspend();

    void onInterrupt();

    void resumeAfterInterrupt();

    void terminated();
}