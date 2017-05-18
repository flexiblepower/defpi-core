package org.flexiblepower.service.serializers;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.flexiblepower.service.exceptions.SerializationException;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Parser;

public class ProtobufMessageSerializer implements MessageSerializer<GeneratedMessage> {

    private final List<Parser<? extends GeneratedMessage>> parsers = new ArrayList<>();
    private final DescriptorType type = DescriptorType.PROTOBUF;

    @Override
    public void addMessageClass(final Class<?> cls) {
        try {
            @SuppressWarnings("unchecked")
            final Parser<? extends GeneratedMessage> parser = (Parser<? extends GeneratedMessage>) cls
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
    public byte[] serialize(final GeneratedMessage object) {
        if (object != null) {
            return object.toByteArray();
        }
        return null;
    }

    @Override
    public GeneratedMessage deserialize(final byte[] data) throws SerializationException {
        for (final Parser<? extends GeneratedMessage> parser : this.parsers) {
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
