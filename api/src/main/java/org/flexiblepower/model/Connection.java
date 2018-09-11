/*-
 * #%L
 * dEF-Pi API
 * %%
 * Copyright (C) 2017 - 2018 Flexible Power Alliance Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.flexiblepower.model;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The connection is how processes communicate with one another. Note that this class is only the model of a
 * connection, and should not contain any implementation specifics.
 *
 * @author coenvl
 * @version 0.1
 * @since Mar 20, 2017
 */
@Data
@Entity
@NoArgsConstructor(force = true, access = AccessLevel.PROTECTED) // Must be generated for Morphia
public class Connection {

    /**
     * A connection endpoint, referring to one interface of a running process.
     *
     * @version 0.1
     * @since Dec 6, 2017
     */
    @Data
    @Embedded
    @NoArgsConstructor(force = true, access = AccessLevel.PROTECTED) // Must be generated for Morphia
    public static class Endpoint {

        /**
         * Construct a connection endpoint for the specified process and interface Id
         *
         * @param processId   The ObjectId of the process this endpoint will use
         * @param interfaceId The ID of the interface this endpoint will use
         */
        public Endpoint(final ObjectId processId, final String interfaceId) {
            this.processId = processId;
            this.interfaceId = interfaceId;
        }

        @JsonSerialize(using = ToStringSerializer.class)
        @JsonDeserialize(using = ObjectIdDeserializer.class)
        private final ObjectId processId;

        private final String interfaceId;

        /**
         * The version that is actually used. The value is set by the
         * ConnectionManager.
         */
        private String interfaceVersionName;

        @Override
        public String toString() {
            return String.format("Endpoint [processId=%s, interface=%s]", this.processId, this.interfaceId);
        }

    }

    @Id
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    private final ObjectId id;

    /**
     * The port where one endpoint will listen, and the other will target
     */
    @JsonIgnore
    private int port;

    @Embedded
    private final Endpoint endpoint1;

    @Embedded
    private final Endpoint endpoint2;

    /**
     * @param oid The objectId of the connection
     * @param ep1 One endpoint
     * @param ep2 Second endpoint
     */
    public Connection(final ObjectId oid, final Endpoint ep1, final Endpoint ep2) {
        this.id = oid;
        this.endpoint1 = ep1;
        this.endpoint2 = ep2;
    }

    /**
     * Returns the other endpoint of the connection. More formally it returns the endpoint e2 for which
     * <code>e.equals(e2)</code> does <i>not</i> hold.
     *
     * @param e the endpoint which we already have
     * @return the <b>other</b> endpoint
     * @throws IllegalArgumentException if the endpoint is not part of the connection at all
     */
    public Endpoint getOtherEndpoint(final Endpoint e) {
        if (e.equals(this.endpoint1)) {
            return this.endpoint2;
        } else if (e.equals(this.endpoint2)) {
            return this.endpoint1;
        } else {
            throw new IllegalArgumentException("The provided endpoint is not part of this connection");
        }
    }

    /**
     * Returns the endpoint of the connection with the provided process. More formally it returns the endpoint e for
     * which
     * <code>e.getProcessId().equals(process)</code> holds.
     *
     * @param process the process to look up
     * @return the endpoint with the provided process
     * @throws IllegalArgumentException if the process is not part of either endpoint
     */
    public Endpoint getEndpointForProcess(final Process process) {
        if (this.endpoint1 != null && this.endpoint1.getProcessId().equals(process.getId())) {
            return this.endpoint1;
        } else if (this.endpoint2 != null && this.endpoint2.getProcessId().equals(process.getId())) {
            return this.endpoint2;
        } else {
            throw new IllegalArgumentException("The provided processId is not part of this connection");
        }
    }

}
