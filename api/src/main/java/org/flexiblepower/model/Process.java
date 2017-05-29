/**
 * File Proces.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.model;

import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Process
 *
 * @author coenvl
 * @version 0.1
 * @since Mar 30, 2017
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Process {

	public static enum ProcessState {
		STARTING, INITIALIZING, RUNNING, SUSPENDED, TERMINATED
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
	 * The NodePool where this process should be running. Mutually exclusive
	 * with privateNodeId.
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
	 * The node on which the process is actually running. May be null when the
	 * state is not RUNNING.
	 */
	private String runningDockerNodeId;

}
