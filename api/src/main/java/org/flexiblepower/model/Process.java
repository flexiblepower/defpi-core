/**
 * File Process.java
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
@NoArgsConstructor // Must be generated for Morphia
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(exclude = {"runningNodeId", "dockerId"})
public class Process {

    /**
     * The ProcessState describes the status of the process
     *
     * @version 0.1
     * @since Dec 6, 2017
     */
    public static enum ProcessState {
    /**
     * The process is starting, meaning its specification exists somewhere in memory, but the docker container
     * hasn't started
     */
    STARTING,
    /**
     * The docker container in which the process will run is running, but the process is still waiting for a
     * configuration
     */
    INITIALIZING,
    /**
     * The process is operational and running
     */
    RUNNING,
    /**
     * The process is suspended, meaning the docker service is removed, but its state still exists in suspended mode
     */
    SUSPENDED,
    /**
     * The process is terminated and will not be resumed, it is ready to be completely deleted from memory
     */
    TERMINATED
    }

    /**
     * A ProcessParameter is a configuration parameter that has a key and a value.
     *
     * @version 0.1
     * @since Dec 6, 2017
     */
    @Value
    @AllArgsConstructor
    @NoArgsConstructor(force = true)
    public static class ProcessParameter {

        private String key;
        private String value;
    }

    /**
     * A mountpoint specifies what volume to mount to a container. This is especially helpful if certain drivers expect
     * e.g. a serial or USB device to be connected to the hardware it is running on.
     *
     * @version 0.1
     * @since Dec 6, 2017
     */
    @Value
    @AllArgsConstructor
    @NoArgsConstructor(force = true)
    public static class MountPoint {

        private String source;
        private String target;
    }

    /**
     * Definition of exposed ports to the outside world, for when a process needs to expose a port for a webservice.
     *
     * @version 0.1
     * @since Mar 20, 2018
     */
    @Value
    @AllArgsConstructor
    @NoArgsConstructor(force = true)
    public static class ExposePort {

        private int internal;
        private int external;
    }

    @Id
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    private ObjectId id;

    // The name is only used for human readable identification.
    private String name;
    
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    private ObjectId userId;

    private String serviceId;

    /**
     * The NodePool where this process should be running. Mutually exclusive with privateNodeId.
     */
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    private ObjectId nodePoolId;

    /**
     * The Private Node where this process should be running. Mutually exclusive with nodePoolId.
     */
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    private ObjectId privateNodeId;
    
    private String token;

    private List<Connection> connections;

    private ProcessState state;

    private String dockerId;

    /**
     * The node on which the process is actually running. May be null when the state is not RUNNING.
     */
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    private ObjectId runningNodeId;

    private List<ProcessParameter> configuration;

    /**
     * To enable debugging of a process, this number should be set to something else than 0. Note that this can only be
     * done when serializing a process from JSON.
     */
    private int debuggingPort;

    private long maxMemoryBytes;

    private long maxNanoCPUs;

    @Builder.Default
    private boolean suspendOnDebug = true;

    /**
     * Mount points can be added in order to allow physical devices be used from the java process. e.g. to use a usb
     * device from /dev/usb0
     */
    private List<MountPoint> mountPoints;

    private List<ExposePort> exposePorts;

}
