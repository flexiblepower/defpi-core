package org.flexiblepower.model;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Entity
public class Connection {

    @Id
    @JsonIgnore
    private final ObjectId id = null;

    private final String process1;
    private final String process2;

    private final String interface1;
    private final String interface2;

}
