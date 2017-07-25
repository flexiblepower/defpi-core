package org.flexiblepower.model;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity
public class Connection {

	@Id
	@JsonSerialize(using = ToStringSerializer.class)
	@JsonDeserialize(using = ObjectIdDeserializer.class)
	private ObjectId id = null;

	@JsonSerialize(using = ToStringSerializer.class)
	@JsonDeserialize(using = ObjectIdDeserializer.class)
	private ObjectId process1Id;

	private String interface1Id;

	@JsonSerialize(using = ToStringSerializer.class)
	@JsonDeserialize(using = ObjectIdDeserializer.class)
	private ObjectId process2Id;

	private String interface2Id;

}
