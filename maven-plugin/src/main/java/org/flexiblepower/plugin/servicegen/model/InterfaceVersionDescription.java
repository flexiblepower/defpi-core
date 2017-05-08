/**
 * File InterfaceVersionDescription.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.plugin.servicegen.model;

/**
 * InterfaceVersionDescription
 *
 * @author coenvl
 * @version 0.1
 * @since May 8, 2017
 */
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"versionName", "type", "location", "sends", "receives"})
public class InterfaceVersionDescription {

    /**
     * (Required)
     */
    @JsonProperty("versionName")
    public String versionName;

    /**
     * (Required)
     */
    @JsonProperty("type")
    public InterfaceVersionDescription.Type type;

    /**
     * (Required)
     */
    @JsonProperty("location")
    public String location;

    /**
     * (Required)
     */
    @JsonProperty("sends")
    @JsonDeserialize(as = java.util.LinkedHashSet.class)
    public Set<String> sends = null;

    /**
     * (Required)
     */
    @JsonProperty("receives")
    @JsonDeserialize(as = java.util.LinkedHashSet.class)
    public Set<String> receives = null;

    public enum Type {

        XSD("xsd"),
        PROTO("proto");

        private final String value;
        private final static Map<String, InterfaceVersionDescription.Type> CONSTANTS = new HashMap<>();

        static {
            for (final InterfaceVersionDescription.Type c : Type.values()) {
                Type.CONSTANTS.put(c.value, c);
            }
        }

        private Type(final String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static InterfaceVersionDescription.Type fromValue(final String value) {
            final InterfaceVersionDescription.Type constant = Type.CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
