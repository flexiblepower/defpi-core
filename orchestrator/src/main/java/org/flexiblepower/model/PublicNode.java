/**
 * File PublicNode.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.model;

import org.mongodb.morphia.annotations.Entity;

/**
 * PublicNode
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 20 mrt. 2017
 */
@Entity
public class PublicNode extends Node {

    public PublicNode() {
        this(null);
    }

    public PublicNode(final String hostname) {
        super(hostname);
    }

}
