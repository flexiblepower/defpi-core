/*-
 * #%L
 * dEF-Pi API
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
package org.flexiblepower.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
 * @version 0.1
 * @since Apr 24, 2017
 */
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({"optional", "array"})
public class Parameter {

    /**
     * The type of the parameter defines how it should be parsed, what values can be assigned, and what type the
     * configuration will return.
     *
     * @version 0.1
     * @since Apr 24, 2017
     */
    public enum Type {
        /**
         * A one bit boolean which is either true or false
         *
         * @see Boolean
         */
        BOOLEAN,
        /**
         * One byte with a range of 0x00 (0) to 0xFF (255, or -128 depending on the used application)
         *
         * @see Byte
         */
        BYTE,
        /**
         * Two bytes representing a unicode character like 'a', '5', '€', 'α' or '\n'
         *
         * @see Character
         */
        CHARACTER,
        /**
         * A two byte short integer, with a range of -32,768 to of 32,767
         *
         * @see Short
         */
        SHORT,
        /**
         * A four byte integer, with a range of -2<sup>31</sup> and a maximum value of 2<sup>31</sup>-1
         *
         * @see Integer
         */
        INTEGER,
        /**
         * An eight byte long integer, with a range of -2<sup>63</sup> and a maximum value of 2<sup>63</sup>-1
         *
         * @see Long
         */
        LONG,
        /**
         * A four byte single precision IEEE 754 floating point number.
         *
         * @see Float
         */
        FLOAT,
        /**
         * An eight byte double precision IEEE 754 floating point number
         *
         * @see Float
         */
        DOUBLE,
        /**
         * A variable length String of characters representing a piece of text
         *
         * @see String
         */
        STRING;

        /**
         * @return The key string that is used to identify this parameter in a key/value map
         */
        @JsonValue
        public String getKey() {
            return this.name().toLowerCase();
        }

        /**
         * Get the string representation of the Java primitive type that corresponds to this parameter type. This is
         * used primarily during code generation.
         *
         * @return A string containing the java primitive or String
         */
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

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private Type type;

    @JsonProperty("isArray")
    private boolean isArray;

    @JsonProperty("isOptional")
    private boolean isOptional;

    @JsonProperty("values")
    private String[] values;

    @JsonProperty("labels")
    private String[] labels;

    @JsonProperty("default")
    private String defaultValue;

}
