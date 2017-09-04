/**
 * File Parameter.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Parameter {

    public enum Type {
        BOOLEAN,
        BYTE,
        CHARACTER,
        SHORT,
        INTEGER,
        LONG,
        FLOAT,
        DOUBLE,
        STRING;

        @JsonValue
        public String getKey() {
            return this.name().toLowerCase();
        }

        public String getJavaTypeName() {
            if (this.equals(STRING)) {
                return "String";
            } else if (this.equals(CHARACTER)) {
                return "char";
            } else if (this.equals(INTEGER)) {
                return "int";
            } else {
                return this.name().toLowerCase();
            }
        }
    }

    private String id;
    private String name;
    private Type type;

    private boolean isArray;
    private boolean isOptional;

    private String[] values;
    private String[] labels;

    @JsonProperty("default")
    private String defaultValue;

}
