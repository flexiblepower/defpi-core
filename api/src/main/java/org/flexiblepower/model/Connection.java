package org.flexiblepower.model;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Entity
public class Connection {

	@Id
	@JsonSerialize(using = ToStringSerializer.class)
	@JsonDeserialize(using = ObjectIdDeserializer.class)
	private final ObjectId id = null;

	@JsonSerialize(using = ToStringSerializer.class)
	@JsonDeserialize(using = ObjectIdDeserializer.class)
	private final ObjectId process1;
	

	@JsonSerialize(using = ToStringSerializer.class)
	@JsonDeserialize(using = ObjectIdDeserializer.class)
	private final ObjectId process2;

	private final String interface1;
	private final String interface2;

}
