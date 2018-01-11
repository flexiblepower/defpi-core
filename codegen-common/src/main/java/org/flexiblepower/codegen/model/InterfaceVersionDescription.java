/**
 * File InterfaceVersionDescription.java
 *
 * Copyright 2017 FAN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flexiblepower.codegen.model;

/**
 * InterfaceVersionDescription
 *
 * @version 0.1
 * @since May 8, 2017
 */
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Getter;
import lombok.Setter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"versionName", "type", "location", "sends", "receives"})
public class InterfaceVersionDescription {

    /**
     * (Required)
     */
    @JsonProperty("versionName")
    private String versionName;

    /**
     * (Required)
     */
    @JsonProperty("type")
    private InterfaceVersionDescription.Type type;

    /**
     * (Required)
     */
    @JsonProperty("location")
    private String location;

    /**
     * (Required)
     */
    @JsonProperty("sends")
    @JsonDeserialize(as = java.util.LinkedHashSet.class)
    private final Set<String> sends = null;

    /**
     * (Required)
     */
    @JsonProperty("receives")
    @JsonDeserialize(as = java.util.LinkedHashSet.class)
    private final Set<String> receives = null;

    @Setter
    @JsonIgnore
    private String hash = null;

    @Setter
    @JsonIgnore
    private String modelPackageName = null;

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
