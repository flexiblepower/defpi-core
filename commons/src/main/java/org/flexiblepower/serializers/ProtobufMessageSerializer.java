package org.flexiblepower.serializers;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.flexiblepower.exceptions.SerializationException;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Parser;

public class ProtobufMessageSerializer<T extends GeneratedMessage> implements MessageSerializer<T> {

    private final List<Parser<T>> parsers = new ArrayList<>();
    private final DescriptorType type = DescriptorType.PROTOBUF;

    @Override
    public void addMessageClass(final Class<? extends T> cls) {
        try {
            @SuppressWarnings("unchecked")
            final Parser<T> parser = (Parser<T>) cls
                    .getMethod("parser").invoke(null);
            this.parsers.add(parser);
        } catch (IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException
                | NoSuchMethodException
                | SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public byte[] serialize(final T object) {
        if (object != null) {
            return object.toByteArray();
        }
        return null;
    }

    @Override
    public T deserialize(final byte[] data) throws SerializationException {
        for (final Parser<T> parser : this.parsers) {
            try {
                return parser.parseFrom(data);
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
