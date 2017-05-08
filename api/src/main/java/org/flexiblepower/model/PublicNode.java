/**
 * File PublicNode.java
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

/**
 * PublicNode
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 20 mrt. 2017
 */
@Entity
@Getter
public class PublicNode extends Node {

    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    private ObjectId nodePoolId;

    public PublicNode() {
        // for Morphia
    }

    public PublicNode(final UnidentifiedNode unidentifiedNode, final NodePool nodePool) {
        super(unidentifiedNode.getDockerId(), unidentifiedNode.getHostname());
        this.nodePoolId = nodePool.getId();
    }

}
