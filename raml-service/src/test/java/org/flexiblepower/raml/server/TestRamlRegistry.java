/*-
 * #%L
 * dEF-Pi REST Orchestrator
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

import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

import org.flexiblepower.proto.RamlProto.RamlRequest;
import org.flexiblepower.proto.RamlProto.RamlRequest.Method;
import org.flexiblepower.proto.RamlProto.RamlResponse;
import org.flexiblepower.raml.server.RamlRequestHandler;
import org.flexiblepower.raml.server.RamlResourceRequest;
import org.flexiblepower.service.ConnectionHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * TestRamlRegistry
 *
 * @version 0.1
 * @since Aug 10, 2019
 */
@SuppressWarnings({"static-method", "javadoc"})
public class TestRamlRegistry {

    final ConnectionHandler handler = new TestConnectionHandler();
    final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void patternTest() {
        Pattern p = RamlResource.getPattern("/test");
        Assertions.assertEquals("/test", p.pattern());
        p = RamlResource.getPattern("/test/{id}/nogiets/{version}/zoiets");
        System.out.println(p);
        Assertions.assertTrue(p.matcher("/test/2-._0~/nogiets/hoihoi.apx#2/zoiets").matches());
    }

    @Test
    public void pathParamsTest() {
        final RamlResource resource = new RamlResource(new Object(), null, "/test/{id}/nogiets/{version}/zoiets");
        final Pattern p = resource.getUriPattern();

        final String requestURI = "/test/21/nogiets/SNAPSHOT/zoiets";
        Assertions.assertTrue(p.matcher(requestURI).matches());
        final RamlResourceRequest request = new RamlResourceRequest(resource, requestURI, null);
        final Map<String, String> params = request.getPathParametersFromUri();
        System.out.println(params);
        Assertions.assertEquals("21", params.get("id"));
        Assertions.assertEquals("SNAPSHOT", params.get("version"));
    }

    @Test
    public void testSimpleGet() throws Exception {
        System.out.println("1");
        final RamlRequest request = RamlRequest.newBuilder().setUri("/example").setMethod(Method.GET).setId(1).build();
        System.out.println("2");
        final RamlResponse response = RamlRequestHandler.handle(this.handler, request);
        System.out.println("3");
        System.out.println(response.getBody().toStringUtf8());
        System.out.println("4");
        Assertions.assertEquals(200, response.getStatus());
        System.out.println("5");
        Assertions.assertEquals("Hello world!", this.mapper.readValue(response.getBody().toByteArray(), String.class));
    }

    @Test
    public void testQueryParam() throws Exception {
        final RamlRequest request = RamlRequest.newBuilder()
                .setUri("/example?name=Maarten")
                .setMethod(Method.GET)
                .setId(1)
                .build();
        final RamlResponse response = RamlRequestHandler.handle(this.handler, request);

        System.out.println(response.getBody().toStringUtf8());
        Assertions.assertEquals(200, response.getStatus());

        Assertions.assertEquals("Hello Maarten!",
                this.mapper.readValue(response.getBody().toByteArray(), String.class));
    }

    @Test
    public void testPathParam() throws Exception {
        final RamlRequest request = RamlRequest.newBuilder()
                .setUri("/example/3")
                .setMethod(Method.GET)
                .setId(1)
                .build();
        final RamlResponse response = RamlRequestHandler.handle(this.handler, request);

        System.out.println(response.getBody().toStringUtf8());
        Assertions.assertEquals(200, response.getStatus());

        Assertions.assertEquals("Hello world!\nHello world!\nHello world!\n",
                this.mapper.readValue(response.getBody().toByteArray(), String.class));
    }

    @Test
    public void testComplicatedRequest() throws Exception {
        final RamlRequest request = RamlRequest.newBuilder()
                .setUri("/example/complicated/100?q=waarde&test=49")
                .setMethod(Method.POST)
                .setId(5)
                .setBody(this.mapper.writeValueAsString(Collections.singletonMap("waarde", "80")))
                .build();
        final RamlResponse response = RamlRequestHandler.handle(this.handler, request);

        System.out.println(response.getBody().toStringUtf8());
        Assertions.assertEquals(200, response.getStatus());

        Assertions.assertEquals(187f, this.mapper.readValue(response.getBody().toByteArray(), Float.class));
    }

}
