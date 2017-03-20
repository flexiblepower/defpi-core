/**
 * File NodePool.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.model;

import java.util.Set;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Reference;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * NodePool
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 20 mrt. 2017
 */
public class NodePool {

    @Id
    @JsonProperty("id")
    private final ObjectId id;

    @JsonProperty("name")
    private final String name;

    @JsonProperty("nodes")
    @Reference
    private Set<Node> nodes;

    public NodePool(final String poolName) {
        this.id = new ObjectId(UUID.randomUUID().toString());
        this.name = poolName;
    }

}
