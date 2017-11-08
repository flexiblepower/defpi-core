/**
 * File Proces.java
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

import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Value;

/**
 * Process
 *
 * @version 0.1
 * @since Mar 30, 2017
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(exclude = {"runningDockerNodeId", "dockerId"})
public class Process {

    public static enum ProcessState {
        STARTING,
        INITIALIZING,
        RUNNING,
        SUSPENDED,
        TERMINATED
    }

    @Value
    @AllArgsConstructor
    @NoArgsConstructor(force = true)
    public static class ProcessParameter {

        private String key;
        private String value;
    }

    @Id
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    private ObjectId id;

    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    private ObjectId userId;

    private String serviceId;

    /**
     * The NodePool where this process should be running. Mutually exclusive with
     * privateNodeId.
     */
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    private ObjectId nodePoolId;

    /**
     * The Private Node where this process should be running. Mutually exclusive
     * with nodePoolId.
     */
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    private ObjectId privateNodeId;

    private List<Connection> connections;

    private ProcessState state;

    private String dockerId;

    /**
     * The node on which the process is actually running. May be null when the state
     * is not RUNNING.
     */
    private String runningDockerNodeId;

    private List<ProcessParameter> configuration;

    /**
     * To enable debugging of a process, this number should be set to something else
     * than 0. Note that this can only be done when serializing a process from JSON.
     */
    private int debuggingPort;
    
    private long maxMemoryBytes;

    private long maxNanoCPUs;
    
    private boolean suspendOnDebug = true;

    /**
     * Mount points can be added in order to allow physical devices be used from the
     * java process. e.g. to use a usb device from /dev/usb0
     */
    private Map<String, String> mountPoints;

}
