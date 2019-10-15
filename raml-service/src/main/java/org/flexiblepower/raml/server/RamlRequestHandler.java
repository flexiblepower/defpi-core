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
package org.flexiblepower.raml.server;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.flexiblepower.proto.RamlProto.RamlRequest;
import org.flexiblepower.proto.RamlProto.RamlRequest.Method;
import org.flexiblepower.proto.RamlProto.RamlResponse;
import org.flexiblepower.service.Connection;
import org.flexiblepower.service.ConnectionHandler;
import org.flexiblepower.service.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
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

    static {
        RamlRequestHandler.mapper.setSerializationInclusion(Include.NON_EMPTY);
    }

    /**
     * Handle a message by invoking the corresponding resource
     *
     * @param handler The ConnectionHandler that will provide the resources, and provided this message
     * @param message The message to handle
     */
    public static void handle(final ConnectionHandler handler, final RamlRequest message) {
        if (!RamlRequestHandler.RESOURCES.containsKey(handler)) {
            RamlRequestHandler.RESOURCES.put(handler, new RamlResourceRegistry(handler));
        }

        // Get the connection to send the request in
        final Connection conn = ConnectionManager.getMyConnection(handler);
        if (conn == null) {
            RamlRequestHandler.log.error("Unable to handle request without return connection");
            return;
        }

        // Start building the response already
        final RamlResponse.Builder builder = RamlResponse.newBuilder().setId(message.getId());
        builder.putHeaders("Server", RamlRequestHandler.class.getSimpleName());

        // Find the request handler for this resource
        final RamlResourceRequest request = RamlRequestHandler.RESOURCES.get(handler).getResourceForMessage(message);
        if (request == null) {
            // TODO set 405 if the location is correct but not the method
            // TODO set 415 if the location and method are correct but the content-type is not correct
            RamlRequestHandler.log.warn("Unable to locate resource for {}", message.getUri());
            RamlRequestHandler.respond(conn,
                    builder.setStatus(404)
                            .setBody(ByteString.copyFromUtf8("No resource found for " + message.getUri()))
                            .build());
            return;
        }

        // Now we actually call our local code
        try {
            final Object o = request.invoke(message);
            // And build the returning response
            int status = 204;
            if (o != null) {
                builder.setBody(ByteString.copyFrom(RamlRequestHandler.mapper.writeValueAsBytes(o)));
                builder.putHeaders(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                status = 200;
            }

            if (((status == 204) && Method.PUT.equals(message.getMethod()))
                    || Method.POST.equals(message.getMethod())) {
                status = 201;
            }

            builder.setStatus(status);
            RamlRequestHandler.respond(conn, builder.build());

        } catch (final InvocationTargetException e) {
            final Throwable cause = e.getCause();
            RamlRequestHandler.log.warn("Error invoking raml resource: {}", cause.getMessage());
            RamlRequestHandler.log.trace(cause.getMessage(), cause);
            if (cause instanceof WebApplicationException) {
                final WebApplicationException wae = (WebApplicationException) cause;
                RamlRequestHandler.respond(conn,
                        builder.setStatus(wae.getResponse().getStatus())
                                .setBody(ByteString.copyFromUtf8(wae.getMessage()))
                                .build());
            } else {
                RamlRequestHandler.respond(conn,
                        builder.setStatus(500)
                                .setBody(ByteString.copyFromUtf8(
                                        cause.getClass().getName() + "::" + RamlRequestHandler.writeException(cause)))
                                .build());
            }
        } catch (final IllegalArgumentException | IllegalAccessException e) {
            RamlRequestHandler.log.warn("Error invoking raml resource: {}", e.getMessage());
            RamlRequestHandler.log.trace(e.getMessage(), e);
            RamlRequestHandler.respond(conn,
                    builder.setStatus(500)
                            .setBody(ByteString
                                    .copyFromUtf8(e.getClass().getName() + "::" + RamlRequestHandler.writeException(e)))
                            .build());
        } catch (final JsonProcessingException e) {
            RamlRequestHandler.log.warn("Unable to create response: {}", e.getMessage());
            RamlRequestHandler.log.trace(e.getMessage(), e);
            RamlRequestHandler.respond(conn,
                    builder.setStatus(500)
                            .setBody(ByteString
                                    .copyFromUtf8(e.getClass().getName() + "::" + RamlRequestHandler.writeException(e)))
                            .build());
        } catch (final Throwable e) {
            RamlRequestHandler.log.warn("Unexpected exception: {}", e.getMessage());
            RamlRequestHandler.log.trace(e.getMessage(), e);
            RamlRequestHandler.respond(conn,
                    builder.setStatus(500)
                            .setBody(ByteString
                                    .copyFromUtf8(e.getClass().getName() + "::" + RamlRequestHandler.writeException(e)))
                            .build());
        }

    }

    private static String writeException(final Throwable e) {
        try {
            return RamlRequestHandler.mapper.writeValueAsString(e);
        } catch (final JsonProcessingException e1) {
            RamlRequestHandler.log.error("Unable to write actual error message!");
            return "{\"exception\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * @param build
     */
    private static void respond(final Connection connection, final RamlResponse response) {
        if (connection.isConnected()) {
            try {
                connection.send(response);
            } catch (final IOException e) {
                RamlRequestHandler.log.error("Unable to respond, exception while sending: {}", e.getMessage());
                RamlRequestHandler.log.error(e.getMessage(), e);
            }
        } else {
            RamlRequestHandler.log.error("Unable to respond, connection is not connected!");
        }
    }

}
