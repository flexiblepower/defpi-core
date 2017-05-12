/**
 * File ObjectIdDeserializer.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.model;

import java.io.IOException;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * ObjectIdDeserializer
 *
 * @author wilco
 * @version 0.1
 * @since May 5, 2017
 */
public class ObjectIdDeserializer extends StdDeserializer<ObjectId> {

    private static final long serialVersionUID = 4378710200595473113L;

    public ObjectIdDeserializer() {
        super(ObjectId.class);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser,
     * com.fasterxml.jackson.databind.DeserializationContext)
     */
    @Override
    public ObjectId deserialize(final JsonParser p, final DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        return new ObjectId(p.getValueAsString());
    }

}
