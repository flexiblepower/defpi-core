/**
 * File Node.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.model;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Reference;

import lombok.Getter;

/**
 * Node
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 20 mrt. 2017
 */
@Getter
public class Node {

    @Id
    private ObjectId id;

    @Reference
    private User owner;

}
