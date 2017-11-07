/**
 * File UnidentifiedNode.java
 *
 * Copyright 2017 FAN
 */
package org.flexiblepower.model;

import org.mongodb.morphia.annotations.Entity;

import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * UnidentifiedNode
 *
 * @version 0.1
 * @since 20 mrt. 2017
 */
@Entity
@ToString
@NoArgsConstructor
public class UnidentifiedNode extends Node {

    public UnidentifiedNode(final String dockerId, final String hostname, final Architecture architecture) {
        super(dockerId, hostname, architecture);
    }

}
