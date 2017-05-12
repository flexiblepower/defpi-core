package org.flexiblepower.service.serializers;

import org.flexiblepower.service.exceptions.SerializationException;

public interface MessageSerializer<T> {

    public enum DescriptorType {
        PROTOBUF,
        XSD,
        JAVAOBJECT,
        NONE
    }

    DescriptorType getType();

    public T deserialize(byte[] data) throws SerializationException;

    public byte[] serialize(T object) throws SerializationException;

}
