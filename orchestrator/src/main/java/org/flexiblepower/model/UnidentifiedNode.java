/**
 * File UnidentifiedNode.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.model;

/**
 * UnidentifiedNode
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 20 mrt. 2017
 */
public class UnidentifiedNode extends Node {

    public UnidentifiedNode() {
        this(null);
    }

    public UnidentifiedNode(final String hostname) {
        super(hostname);
    }

}
