/**
 * File Parameter.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Parameter
 *
 * @author coenvl
 * @version 0.1
 * @since Apr 24, 2017
 */
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Parameter {

    enum Type {
        BOOLEAN,
        BYTE,
        CHAR,
        SHORT,
        INTEGER,
        LONG,
        FLOAT,
        DOUBLE,
        STRING
    }

    private String id;
    private String name;
    private Type getType;
    private boolean isArray;
    private boolean isOptional;

    private String[] optionValues;
    private String[] optionLabel;

    private String getDefaultValue;

}
