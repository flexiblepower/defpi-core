/**
 * File UnidentifiedNode.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.model;

import org.mongodb.morphia.annotations.Entity;

import lombok.Getter;

/**
 * UnidentifiedNode
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 20 mrt. 2017
 */
@Entity
@Getter
public class UnidentifiedNode extends Node {

    public UnidentifiedNode() {
        // for Morphia
    }

    public UnidentifiedNode(final String dockerId, final String hostname) {
        super(dockerId, hostname);
    }
}
