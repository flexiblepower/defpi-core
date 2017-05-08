/**
 * File Node.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.model;

import java.util.Date;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.Getter;
import lombok.Setter;

/**
 * Node
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 20 mrt. 2017
 */
@Getter
public abstract class Node {

	public static enum DockerNodeStatus {
		MISSING, UNKNOWN, DOWN, READY, DISCONNECTED;

		public static DockerNodeStatus fromString(final String text) {
			for (final DockerNodeStatus s : DockerNodeStatus.values()) {
				if (s.toString().equalsIgnoreCase(text)) {
					return s;
				}
			}
			return UNKNOWN;
		}
	}

	@Id
	@JsonSerialize(using = ToStringSerializer.class)
	@JsonDeserialize(using = ObjectIdDeserializer.class)
	protected ObjectId id;

	protected String dockerId;

	@Setter
	protected String hostname;

	/**
	 * The last time the status was retrieved from docker
	 */
	@Setter
	protected Date lastSync;

	@Setter
	protected DockerNodeStatus status;

	@Setter
	protected Architecture architecture;

	public Node() {
		// for Morphia
	}

	public Node(final String dockerId, final String hostname, Architecture architecture) {
		this.dockerId = dockerId;
		this.hostname = hostname;
		this.status = DockerNodeStatus.UNKNOWN;
		this.architecture = architecture;
		this.lastSync = new Date(); // now
	}

}
