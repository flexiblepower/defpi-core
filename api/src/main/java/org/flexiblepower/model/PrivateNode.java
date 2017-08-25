/**
 * File Node.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.model;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.Getter;
import lombok.ToString;

/**
 * Node
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 20 mrt. 2017
 */
@Entity
@Getter
@ToString
public class PrivateNode extends Node {

    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    private ObjectId userId;

    public PrivateNode() {
        // for Morphia
    }

    public PrivateNode(final UnidentifiedNode unidentifiedNode, final User owner) {
        super(unidentifiedNode.getDockerId(), unidentifiedNode.getHostname(), unidentifiedNode.getArchitecture());
        this.userId = owner.getId();
    }

}
