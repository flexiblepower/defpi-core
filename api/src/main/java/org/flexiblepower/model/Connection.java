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

package org.flexiblepower.model;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor(force = true)
public class Connection {

    @Data
    @Embedded
    @NoArgsConstructor(force = true)
    public static class Endpoint {

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
    private ObjectId id;

    /**
     * The port where one endpoint will listen, and the other will target
     */
    @JsonIgnore
    private int port;
    
    @Embedded
    private final Endpoint endpoint1;

    @Embedded
    private final Endpoint endpoint2;

    public Connection(final ObjectId oid, Endpoint ep1, Endpoint ep2) {
        this.id = oid;
        this.endpoint1 = ep1;
        this.endpoint2 = ep2;
    }
    
    public Endpoint getOtherEndpoint(final Endpoint e) {
        if (e.equals(this.endpoint1)) {
            return this.endpoint2;
        } else if (e.equals(this.endpoint2)) {
            return this.endpoint1;
        } else {
            throw new IllegalArgumentException("The provided endpoint is not part of this connection");
        }
    }

    public Endpoint getEndpointForProcess(final Process process) {
        if (process.getId().equals(this.endpoint1.getProcessId())) {
            return this.endpoint1;
        } else if (process.getId().equals(this.endpoint2.getProcessId())) {
            return this.endpoint2;
        } else {
            throw new IllegalArgumentException("The provided processId is not part of this connection");
        }
    }

}
