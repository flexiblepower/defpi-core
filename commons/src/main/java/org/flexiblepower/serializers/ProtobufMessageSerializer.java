/**
 * File ProtobufMessageSerializer.java
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.flexiblepower.exceptions.SerializationException;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;

/**
 * This is the serializer for ProtoBuf based messages.
 *
 * ProtoBuf does not encode what type of message is encoded. In order to solve
 * that, this serializer adds this information to the resulting byte array, and
 * expects this information available for incoming byte arrays. The first byte
 * encodes the length of the name of the byte array. The following bytes are the
 * String representing the name of the byte array. All data that follows is the
 * ProtoBuf encoded data.
 */
public class ProtobufMessageSerializer implements MessageSerializer<Message> {

    private final Map<String, Class<? extends Message>> messageTypes = new HashMap<>();
    private final DescriptorType type = DescriptorType.PROTOBUF;

    @Override
    public void addMessageClass(final Class<? extends Message> cls) {
        if (cls.getSimpleName().length() >= 128) {
            throw new IllegalArgumentException("The name of the message must be less than 128 characters");
        }
        this.messageTypes.put(cls.getSimpleName(), cls);
    }

    @Override
    public byte[] serialize(final Message msg) throws SerializationException {
        if (msg != null) {
            final String msgTypeName = msg.getClass().getSimpleName();
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                baos.write((byte) msgTypeName.length());
                baos.write(msgTypeName.getBytes());
                baos.write(msg.toByteArray());
                // no point closing a ByteArrayOutputStream
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
            return baos.toByteArray();
        }
        return null;
    }

    @Override
    public Message deserialize(final byte[] data) throws SerializationException {
        final int msgTypeNameLength = data[0];
        if (msgTypeNameLength <= 0) {
            throw new SerializationException("Received data is not a valid message: " + Arrays.toString(data));
        }
        if (data.length < (msgTypeNameLength + 2)) {
            throw new SerializationException("Received data is not a valid message: " + Arrays.toString(data));
        }
        final String msgTypeName = new String(data, 1, msgTypeNameLength);
        try {
            if (!this.messageTypes.containsKey(msgTypeName)) {
                throw new SerializationException("Unable to find parser for message type '" + msgTypeName
                        + "', message type was not registered");
            }
            final Parser<?> parser = (Parser<?>) this.messageTypes.get(msgTypeName).getMethod("parser").invoke(null);
            return (Message) parser.parseFrom(data, 1 + msgTypeNameLength, data.length - 1 - msgTypeNameLength);
        } catch (final InvalidProtocolBufferException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException
                | NoSuchMethodException
                | SecurityException e) {
            throw new SerializationException(
                    "Unable to find parser for message type '" + msgTypeName + "', problem during parsing");
        }
    }

    @Override
    public DescriptorType getType() {
        return this.type;
    }

}
