package org.flexiblepower.model;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity
public class Connection {

	@Embedded
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@EqualsAndHashCode
	public static class Endpoint {
		@JsonSerialize(using = ToStringSerializer.class)
		@JsonDeserialize(using = ObjectIdDeserializer.class)
		private ObjectId processId;

		private String interfaceId;

		@Setter
		private String interfaceVersionName;

		@Setter
		private int listenPort;

	}

	@Id
	@JsonSerialize(using = ToStringSerializer.class)
	@JsonDeserialize(using = ObjectIdDeserializer.class)
	private ObjectId id = null;

	@Embedded
	private Endpoint endpoint1;

	@Embedded
	private Endpoint endpoint2;

	public Endpoint getOtherEndpoint(Endpoint e) {
		if (e.equals(endpoint1)) {
			return endpoint2;
		} else if (e.equals(endpoint2)) {
			return endpoint1;
		} else {
			throw new IllegalArgumentException("The provided endpoint is not part of this connection");
		}
	}

	public Endpoint getEndpointForProcess(ObjectId processId) {
		if (processId.equals(endpoint1.getProcessId())) {
			return endpoint1;
		} else if (processId.equals(endpoint2.getProcessId())) {
			return endpoint2;
		} else {
			throw new IllegalArgumentException("The provided processId is not part of this connection");
		}
	}

}
