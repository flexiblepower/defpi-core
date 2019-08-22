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
package org.flexiblepower.raml;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.flexiblepower.proto.RamlProto.RamlRequest;
import org.flexiblepower.proto.RamlProto.RamlResponse;
import org.flexiblepower.service.ConnectionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;

/**
 * RamlRequestHandler
 *
 * @version 0.1
 * @since Aug 9, 2019
 */
public class RamlRequestHandler {

    private static final Logger log = LoggerFactory.getLogger(RamlRequestHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static Map<ConnectionHandler, RamlResourceRegistry> RESOURCES = new HashMap<>();

    /**
     * Handle a message by invoking the corresponding resource
     *
     * @param handler The ConnectionHandler that will provide the resources, and provided this message
     * @param message The message to handle
     * @return A RamlResponse message
     */
    public static RamlResponse handle(final ConnectionHandler handler, final RamlRequest message) {
        if (!RamlRequestHandler.RESOURCES.containsKey(handler)) {
            RamlRequestHandler.RESOURCES.put(handler, new RamlResourceRegistry(handler));
        }

        final RamlResponse.Builder builder = RamlResponse.newBuilder().setId(message.getId());

        final RamlResourceRequest request = RamlRequestHandler.RESOURCES.get(handler).getResourceForMessage(message);
        if (request == null) {
            RamlRequestHandler.log.warn("Unable to locate resource, ignoring message");
            return builder.setStatus(404)
                    .setBody(ByteString.copyFromUtf8("No resource found for " + message.getUri()))
                    .build();
        }

        try {
            final Object o = request.invoke(message);

            if (o != null) {
                builder.setStatus(200).setBody(ByteString.copyFrom(RamlRequestHandler.mapper.writeValueAsBytes(o)));
            } else {
                builder.setStatus(204);
            }

            return builder.build();
        } catch (final IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            RamlRequestHandler.log.warn("Error invoking raml resource: {}", e.getMessage());
            RamlRequestHandler.log.trace(e.getMessage(), e);
            return builder.setStatus(500)
                    .setBody(ByteString.copyFromUtf8(
                            e.getClass().getSimpleName() + " while invoking raml resource: " + e.getMessage()))
                    .build();
        } catch (final JsonProcessingException e) {
            RamlRequestHandler.log.warn("Unable to create response: {}", e.getMessage());
            RamlRequestHandler.log.trace(e.getMessage(), e);
            return builder.setStatus(500)
                    .setBody(ByteString
                            .copyFromUtf8(e.getClass().getSimpleName() + " while creating response: " + e.getMessage()))
                    .build();
        }

    }

}
