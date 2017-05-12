/**
 * File Connection.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

/**
 * Connection
 *
 * @author coenvl
 * @version 0.1
 * @since Apr 24, 2017
 */
public interface Connection {

    public static enum ConnectionState {
        STARTING,
        CONNECTED,
        SUSPENDED,
        INTERRUPTED,
        TERMINATED;
    }

    public void send(byte[] message);

    public ConnectionState getState();

}
