/**
 * File Node.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.model;

import org.mongodb.morphia.annotations.Entity;

import lombok.Getter;

/**
 * Node
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 20 mrt. 2017
 */
@Getter
@Entity
public class PrivateNode extends Node {

    private final String owner;

    public PrivateNode() {
        this(null, null);
    }

    public PrivateNode(final String hostname, final String owner) {
        super(hostname);
        this.owner = owner;
    }

}
