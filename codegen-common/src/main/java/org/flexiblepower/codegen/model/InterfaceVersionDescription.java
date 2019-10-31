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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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

    @JsonProperty("sends")
    @JsonDeserialize(as = TreeSet.class)
    private Set<String> sends = null;

    @JsonProperty("receives")
    @JsonDeserialize(as = TreeSet.class)
    private Set<String> receives = null;

    /**
     * The interface role is only specified for interfaces of the RAML type. If it is used the role will specify what
     * role the service plays in the interface
     */
    @JsonProperty("interfaceRole")
    private InterfaceVersionDescription.Role interfaceRole;

    @Setter
    @JsonIgnore
    private String hash = null;

    @Setter
    @JsonIgnore
    private String modelPackageName = null;

    @Getter
    @Setter
    @JsonIgnore
    private List<String> ramlResources = Collections.emptyList();

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
     * @return A Set of Strings indicating the types of objects that the interface receives. If the interface is of type
     *         RAML, none are defined, so we just put the Raml types
     */
    final public Set<String> getReceives() {
        if ((this.receives == null) && (this.getType() == Type.RAML)) {
            // This also means that it will only be populated the first time
            this.receives = Collections
                    .singleton(Role.CLIENT.equals(this.interfaceRole) ? "RamlResponse" : "RamlRequest");
        }
        return this.receives;
    }

    /**
     * @return A Set of Strings indicating the types of objects that the interface sends. If the interface is of type
     *         RAML, none are defined, so we just put the Raml types
     */
    final public Set<String> getSends() {
        if ((this.sends == null) && (this.getType() == Type.RAML)) {
            // This also means that it will only be populated the first time
            this.sends = Collections.singleton(Role.CLIENT.equals(this.interfaceRole) ? "RamlRequest" : "RamlResponse");
        }
        return this.sends;
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

    /**
     * Role
     *
     * @version 0.1
     * @since Aug 2, 2019
     */
    public enum Role {
        /**
         * Indicates that the service acts as a server in the interface, i.e. it will offer the functions described in
         * the RAML file
         */
        SERVER("server"),
        /**
         * Indicates that the service acts as a cliet in the interface, i.e. it will use the functions described in
         * the RAML file
         */
        CLIENT("client");

        private final String value;
        private final static Map<String, InterfaceVersionDescription.Role> CONSTANTS = new HashMap<>();

        static {
            for (final InterfaceVersionDescription.Role c : Role.values()) {
                Role.CONSTANTS.put(c.value, c);
            }
        }

        private Role(final String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        /**
         * @return The value of the role
         */
        @JsonValue
        public String value() {
            return this.value;
        }

        /**
         * Get the enum type from a string representation of the value
         *
         * @param value The value to get the Role of
         * @return A enum Role that represents the provided value
         */
        @JsonCreator
        public static InterfaceVersionDescription.Role fromValue(final String value) {
            final InterfaceVersionDescription.Role constant = Role.CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }
    }

}
