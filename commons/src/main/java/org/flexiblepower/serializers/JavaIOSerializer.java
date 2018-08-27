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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.flexiblepower.exceptions.SerializationException;

/**
 * JavaIOSerializer
 *
 * @version 0.1
 * @since May 10, 2017
 */
public class JavaIOSerializer implements MessageSerializer<Serializable> {

    private static final DescriptorType type = DescriptorType.JAVAOBJECT;

    @Override
    public byte[] serialize(final Serializable obj) throws SerializationException {
        try (
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                final ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
            return baos.toByteArray();
        } catch (final IOException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public Serializable deserialize(final byte[] ba) throws SerializationException {
        try (
                final ByteArrayInputStream bais = new ByteArrayInputStream(ba);
                final ObjectInputStream oos = new ObjectInputStream(bais)) {
            return (Serializable) oos.readObject();
        } catch (ClassNotFoundException | IOException e) {
            throw new SerializationException(e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.serializers.MessageSerializer#getType()
     */
    @Override
    public DescriptorType getType() {
        return JavaIOSerializer.type;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.service.serializers.MessageSerializer#addMessageClass(java.lang.Class)
     */
    @Override
    public void addMessageClass(final Class<? extends Serializable> clazz) {
        // It's okay do nothing
    }

}
