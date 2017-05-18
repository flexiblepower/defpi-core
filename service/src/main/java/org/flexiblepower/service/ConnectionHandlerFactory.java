/**
 * File ConnectionHandlerFactory.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

/**
 * The ConnectionHandlerFactory is used by a service to provide the library with the object that can act on changing
 * connection statuses and incoming messages.
 *
 * @author coenvl
 * @version 0.1
 * @since May 18, 2017
 */
public interface ConnectionHandlerFactory {

    public ConnectionHandler build();

}