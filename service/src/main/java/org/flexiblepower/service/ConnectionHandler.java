/**
 * File ConnectionHandler.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

/**
 * The ConnectionHandler provides the functionality to deal with changing connection statuses.
 *
 * Specific functions that deal with incoming messages will be called when the messages are received through the
 * connection. Reflection is used to determine any added functions to this interface, and will be called when object are
 * received, if the type of incoming message matches the interface method parameter type.
 *
 * @author coenvl
 * @version 0.1
 * @since Apr 24, 2017
 */
public interface ConnectionHandler {

    void onConnected(Connection connection);

    void onSuspend();

    void resumeAfterSuspend();

    void onInterrupt();

    void resumeAfterInterrupt();

    void terminated();

}