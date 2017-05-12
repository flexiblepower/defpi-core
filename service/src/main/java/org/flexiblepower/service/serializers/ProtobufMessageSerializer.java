package org.flexiblepower.service.serializers;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.flexiblepower.service.exceptions.SerializationException;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Parser;

public class ProtobufMessageSerializer<T extends GeneratedMessage> implements MessageSerializer<T> {

    private final List<Parser<T>> parsers = new ArrayList<>();
    private final DescriptorType type = DescriptorType.PROTOBUF;

    public ProtobufMessageSerializer(final Class<T>... classes) {
        try {
            for (final Class<T> cls : classes) {
                this.parsers.add((Parser<T>) cls.getMethod("parser").invoke(null));
            }
        } catch (IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException
                | NoSuchMethodException
                | SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public byte[] serialize(final GeneratedMessage object) {
        if (object != null) {
            return object.toByteArray();
        }
        return null;
    }

    @Override
    public T deserialize(final byte[] data) throws SerializationException {
        for (final Parser<? extends GeneratedMessage> parser : this.parsers) {
            try {
                return (T) parser.parseFrom(data);
            } catch (final InvalidProtocolBufferException e) {
                // Incorrect parser for this message
            }
        }
        throw new SerializationException("Unable to find parser for message");
    }

    @Override
    public DescriptorType getType() {
        return this.type;
    }

}
