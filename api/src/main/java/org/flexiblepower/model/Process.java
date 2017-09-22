/**
 * File Proces.java
 *
 * Copyright 2017 TNO
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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Value;

/**
 * Process
 *
 * @author coenvl
 * @version 0.1
 * @since Mar 30, 2017
 */
/**
 * @author coenvl
 *
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    private boolean suspendOnDebug = true;

    /**
     * Mount points can be added in order to allow physical devices be used from the
     * java process. e.g. to use a usb device from /dev/usb0
     */
    private Map<String, String> mountPoints;
}
