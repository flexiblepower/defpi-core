/**
 * File JavaIOSerializer.java
 *
 * Copyright 2017 FAN
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
