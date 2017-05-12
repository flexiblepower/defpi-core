/**
 * File Parameter.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.model;

/**
 * Parameter
 *
 * @author coenvl
 * @version 0.1
 * @since Apr 24, 2017
 */
public interface Parameter {

    enum Type {
        BOOLEAN,
        INTEGER,
        FLOAT,
        STRING
    }

    String getId();

    String getName();

    Type getType();

    boolean isArray();

    boolean isOptional();

    String[] optionValues();

    String[] optionLabels();

    String getDefaultValue();

}
