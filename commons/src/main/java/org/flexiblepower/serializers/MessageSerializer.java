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
