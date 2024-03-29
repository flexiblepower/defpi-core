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
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.flexiblepower.exceptions.SerializationException;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * XSDMessageSerializer
 *
 * @version 0.1
 * @since May 18, 2017
 */
public class XSDMessageSerializer implements MessageSerializer<Object> {

    private final Map<String, Class<?>> classes = new HashMap<>();
    private final DescriptorType type = DescriptorType.XSD;
    private Marshaller marshaller;
    private Unmarshaller unmarshaller;

    @Override
    public void addMessageClass(final Class<?> cls) {
        this.classes.put(cls.getName(), cls);
    }

    @Override
    public byte[] serialize(final Object object) throws SerializationException {
        if (!this.classes.values().contains(object.getClass())) {
            throw new IllegalArgumentException(
                    "Message type " + object.getClass().getName() + " is not registered with this MessageSerializer");
        }
        try {
            final Class<?> cls = this.classes.get(object.getClass().getName());
            return this.serialize(object, cls);
        } catch (final JAXBException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public Object deserialize(final byte[] data) throws SerializationException {
        if (this.unmarshaller == null) {
            try {
                this.init();
            } catch (final JAXBException e) {
                throw new SerializationException(e);
            }
        }

        try {
            return this.unmarshaller.unmarshal(new ByteArrayInputStream(data));
        } catch (final JAXBException e) {
            throw new SerializationException("Was not able to deserialize: " + e.getMessage());
        }
    }

    @Override
    public DescriptorType getType() {
        return this.type;
    }

    @SuppressWarnings("unchecked")
    private <T> byte[] serialize(final Object object, final Class<T> cls) throws JAXBException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (this.marshaller == null) {
            this.init();
        }

        if (cls.isAnnotationPresent(XmlRootElement.class)) {
            this.marshaller.marshal(object, baos);
        } else {
            this.marshaller.marshal(
                    new JAXBElement<>(new QName("", cls.getAnnotation(XmlType.class).name()), cls, null, (T) object),
                    baos);
        }
        return baos.toByteArray();
    }

    /**
     * Lazy initialization to avoid having the constructor throw an exception
     *
     * @throws JAXBException If there is any error while instantiating the marshaller or the unmarshaller
     */
    private void init() throws JAXBException {
        final Class<?>[] array = new Class<?>[this.classes.size()];
        final JAXBContext ctx = JAXBContext.newInstance(this.classes.values().toArray(array));
        this.unmarshaller = ctx.createUnmarshaller();
        this.marshaller = ctx.createMarshaller();
    }

}
