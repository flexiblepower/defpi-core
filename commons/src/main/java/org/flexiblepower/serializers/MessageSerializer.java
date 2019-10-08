/*-
 * #%L
 * dEF-Pi commons Library
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
package org.flexiblepower.serializers;

import org.flexiblepower.exceptions.SerializationException;

/**
 * MessageSerializer
 *
 * @version 0.1
 * @since May 18, 2017
 * @param <T> the Type of message to serialize
 */
public interface MessageSerializer<T> {

    /**
     * DescriptorType
     *
     * @version 0.1
     * @since May 18, 2017
     */
    public enum DescriptorType {
        /**
         * A google protobuf type
         */
        PROTOBUF,
        /**
         * A schema validate XML type
         */
        XSD,
        /**
         * A serialized java object
         */
        JAVAOBJECT,
        /**
         * A raml serialized object
         */
        RAML
    }

    /**
     * @return the type of message this the serializer will convert to/from
     */
    DescriptorType getType();

    /**
     * Adds a message class to the set of classes this serializer will convert to/from
     *
     * @param clazz The type of message to add to the list of serializable classes
     */
    public void addMessageClass(Class<? extends T> clazz);

    /**
     * Deserialize a byte array of raw data to an object of Type T. The exact class of the return type will have to be
     * added using {@link #addMessageClass(Class)} before this function is called.
     *
     * @param data the raw byte array
     * @return A proper deserialized object
     * @throws SerializationException When an exception occurs during message (de)serialization
     */
    public T deserialize(byte[] data) throws SerializationException;

    /**
     * Serialize a an object of Type T into a byte array of raw data. The exact class of the argument type will have to
     * be added using {@link #addMessageClass(Class)} before this function is called.
     *
     * @param object to be serialized
     * @return The raw byte array
     * @throws SerializationException When an exception occurs during message (de)serialization
     */
    public byte[] serialize(T object) throws SerializationException;

}
