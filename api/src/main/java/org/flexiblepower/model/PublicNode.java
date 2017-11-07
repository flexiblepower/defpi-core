/**
 * File PublicNode.java
 *
 * Copyright 2017 FAN
 */
package org.flexiblepower.model;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * PublicNode
 *
 * @version 0.1
 * @since 20 mrt. 2017
 */
@Entity
@Getter
@NoArgsConstructor
@ToString(callSuper = true)
public class PublicNode extends Node {

    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    private ObjectId nodePoolId;

    public PublicNode(final UnidentifiedNode unidentifiedNode, final NodePool nodePool) {
        super(unidentifiedNode.getDockerId(), unidentifiedNode.getHostname(), unidentifiedNode.getArchitecture());
        this.nodePoolId = nodePool.getId();
    }

}
