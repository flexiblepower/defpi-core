/**
 * File Connection.java
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

import org.flexiblepower.exceptions.SerializationException;
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
     *
     * @param message the object to be send over the connection.
     * @throws IOException when the state is not connected, or if the connection back end fails to send the message
     * @throws SerializationException when serialization fails just before sending the message
     */
    public void send(Object message) throws IOException;

    /**
     * Indicates whether or not the connection is connected to the other process.
     *
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
     *
     * @return the current state of the connection.
     */
    public ConnectionState getState();

    /**
     * Returns the remote process identifier of the current process.
     *
     * @return the remote process identifier.
     */
    public String remoteProcessId();

    /**
     * Returns the remote service identifier of the service corresponding to the current process.
     *
     * @return the remote service identifier.
     */
    public String remoteServiceId();

    /**
     * Returns the remote interface identifier corresponding to the interface of the service this connection is attached
     * to.
     *
     * @return the remote interface identifier.
     */
    public String remoteInterfaceId();

}