/**
 * File Connection.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import org.flexiblepower.proto.ConnectionProto.ConnectionState;

/**
 * Connection
 *
 * @author coenvl
 * @version 0.1
 * @since Apr 24, 2017
 */
public interface Connection {

    public void send(Object message);

    public boolean isConnected();

    public ConnectionState getState();

}
