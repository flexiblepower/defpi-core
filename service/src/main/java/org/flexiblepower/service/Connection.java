/**
 * File Connection.java
 *
 * Copyright 2017 FAN
 */
package org.flexiblepower.service;

import org.flexiblepower.proto.ConnectionProto.ConnectionState;

/**
 * Connection
 *
 * @version 0.1
 * @since Apr 24, 2017
 */
public interface Connection {

    /**
     * Sends Object message over the connection to the other process.
     * @param message the object to be send over the connection.
     */
    public void send(Object message);

    /**
     * Indicates whether or not the connection is connected to the other process.
     * @return true if connected correctly, otherwise false.
     */
    public boolean isConnected();

    /**
     * Returns the state of the connection in the form of a Protocol Buffers enum.
     * Possible values are:
     * STARTING
     * CONNECTED
     * SUSPENDED
     * INTERRUPTED
     * TERMINATED
     * @return the current state of the connection.
     */
    public ConnectionState getState();

    /**
     * Returns the remote process identifier of the current process.
     * @return the remote process identifier.
     */
    public String remoteProcessId();

    /**
     * Returns the remote service identifier of the service corresponding to the current process.
     * @return the remote service identifier.
     */
    public String remoteServiceId();

    /**
     * Returns the remote interface identifier corresponding to the interface of the service this connection is attached
     * to.
     * @return the remote interface identifier.
     */
    public String remoteInterfaceId();

}