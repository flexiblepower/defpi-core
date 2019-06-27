/*-
 * #%L
 * dEF-Pi service codegen-common
 * %%
 * Copyright (C) 2017 - 2018 Flexible Power Alliance Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.flexiblepower.codegen.model;

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

/**
 * InterfaceVersionDescription
 *
 * @version 0.1
 * @since May 8, 2017
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"versionName", "type", "location", "sends", "receives"})
public class InterfaceVersionDescription {

    /**
     * (Required)
     */
    @JsonProperty("versionName")
    private String versionName;

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
    @JsonDeserialize(as = java.util.TreeSet.class)
    private final Set<String> sends = null;

    /**
     * (Required)
     */
    @JsonProperty("receives")
    @JsonDeserialize(as = java.util.TreeSet.class)
    private final Set<String> receives = null;

    @Setter
    @JsonIgnore
    private String hash = null;

    @Setter
    @JsonIgnore
    private String modelPackageName = null;

    /**
     * @return The Type of the descriptor. If none is given in the service description, determine it automatically from
     *         the file name (if possible).
     */
    final public Type getType() {
        return this.type != null ? this.type : this.determineType();
    }

    /**
     * @return
     */
    private Type determineType() {
        final String[] parts = this.location.split("\\.");
        return Type.fromValue(parts[parts.length - 1]);
    }

    /**
     * The type of descriptor for the interface version. In the current implementation this is either XSD or PROTO, but
     * may be extended in the future.
     *
     * @version 0.1
     * @since May 8, 2017
     */
    public enum Type {

        /**
         * XML Schema Definition descriptor type
         */
        XSD("xsd"),
        /**
         * Google Protobuf descriptor type
         */
        PROTO("proto"),
        /**
         * RAML descriptor type
         */
        RAML("raml");

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

        /**
         * @return The value of the type
         */
        @JsonValue
        public String value() {
            return this.value;
        }

        /**
         * Get the enum type from a string representation of the value
         *
         * @param value The value to get the Type of
         * @return A enum Type that represents the provided value
         */
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
