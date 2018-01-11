/**
 * File MessageSerializer.java
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

package org.flexiblepower.serializers;

import org.flexiblepower.exceptions.SerializationException;

public interface MessageSerializer<T> {

    public enum DescriptorType {
        PROTOBUF,
        XSD,
        JAVAOBJECT,
        NONE
    }

    DescriptorType getType();

    public void addMessageClass(Class<? extends T> clazz);

    public T deserialize(byte[] data) throws SerializationException;

    public byte[] serialize(T object) throws SerializationException;

}
