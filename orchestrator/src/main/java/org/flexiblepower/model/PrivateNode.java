/**
 * File Node.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.model;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;

import lombok.Getter;

/**
 * Node
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 20 mrt. 2017
 */
@Entity
@Getter
public class PrivateNode extends Node {

    private ObjectId userId;

    public PrivateNode() {
        // for Morphia
    }

    public PrivateNode(final UnidentifiedNode unidentifiedNode, final User owner) {
        super(unidentifiedNode.getDockerId(), unidentifiedNode.getHostname());
        this.userId = owner.getId();
    }

}
