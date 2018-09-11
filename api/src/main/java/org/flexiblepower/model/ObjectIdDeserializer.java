/*-
 * #%L
 * dEF-Pi API
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
package org.flexiblepower.model;

import java.io.IOException;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * The ObjectIdDeserializer is used by Jackson to deserialize a string into an ObjectId as part of a JSON object.
 *
 * @version 0.1
 * @since May 5, 2017
 */
public class ObjectIdDeserializer extends StdDeserializer<ObjectId> {

    private static final long serialVersionUID = 4378710200595473113L;

    /**
     * Create an ObjectIdSerializer to deserialize an ObjectId
     */
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
    public ObjectId deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
        return new ObjectId(p.getValueAsString());
    }

}
