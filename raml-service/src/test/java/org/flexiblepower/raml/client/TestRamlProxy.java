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
package org.flexiblepower.raml.client;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.flexiblepower.proto.RamlProto.RamlRequest.Method;
import org.flexiblepower.raml.server.TestConnectionHandler;
import org.flexiblepower.raml.server.TestConnectionHandler.Example;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * TestRamlProxy
 *
 * @version 0.1
 * @since Aug 26, 2019
 */
@SuppressWarnings({"javadoc"})
public class TestRamlProxy {

    final TestConnection connection = new TestConnection();
    final ObjectMapper mapper = new ObjectMapper();
    final ExecutorService executor = Executors.newSingleThreadExecutor();
    final Example ex = RamlProxyClient.generateClient(TestConnectionHandler.Example.class, this.connection);

    @Test
    public void runTest() throws InterruptedException {
        // Have to run it asynchronously because it will block forever until it receives a response
        final Future<?> f = this.executor.submit(this.ex::getExampleText);
        Thread.sleep(100);
        f.cancel(true);

        Assertions.assertEquals("/example", this.connection.getLastMessage().getUri());
        Assertions.assertEquals(Method.GET, this.connection.getLastMessage().getMethod());
        Assertions.assertFalse(this.connection.getLastMessage().hasBody());
    }

    @Test
    public void runQueryParamTest() throws InterruptedException {
        // Have to run it asynchronously because it will block forever until it receives a response
        final Future<?> f = this.executor.submit(() -> this.ex.getPersonalText("Harry"));
        Thread.sleep(100);
        f.cancel(true);

        Assertions.assertEquals("/example?name=Harry", this.connection.getLastMessage().getUri());
        Assertions.assertEquals(Method.GET, this.connection.getLastMessage().getMethod());
        Assertions.assertFalse(this.connection.getLastMessage().hasBody());
    }

    @Test
    public void runPathParamTest() throws InterruptedException {
        // Have to run it asynchronously because it will block forever until it receives a response
        final Future<?> f = this.executor.submit(() -> this.ex.getPersonalText(5));
        Thread.sleep(100);
        f.cancel(true);

        Assertions.assertEquals("/example/5", this.connection.getLastMessage().getUri());
        Assertions.assertEquals(Method.GET, this.connection.getLastMessage().getMethod());
        Assertions.assertFalse(this.connection.getLastMessage().hasBody());
    }

    @Test
    public void runComplexTest() throws InterruptedException, JsonMappingException, JsonProcessingException {
        // Have to run it asynchronously because it will block forever until it receives a response
        final Future<?> f = this.executor
                .submit(() -> this.ex.setStuff(187, "query", Math.PI, Collections.singletonMap("ape", "nut")));
        Thread.sleep(100);
        f.cancel(true);

        final String messageUri = this.connection.getLastMessage().getUri();
        Assertions.assertEquals(55, messageUri.length());
        Assertions.assertEquals("/example/complicated/187?", messageUri.substring(0, 25));
        Assertions.assertTrue(messageUri.contains("test=3.141592653589793"));
        Assertions.assertTrue(messageUri.contains("q=query"));
        Assertions.assertTrue(messageUri.contains("&"));
        Assertions.assertEquals(Method.POST, this.connection.getLastMessage().getMethod());
        Assertions.assertTrue(this.connection.getLastMessage().hasBody());

        @SuppressWarnings("unchecked")
        final Map<String, String> body = this.mapper.readValue(this.connection.getLastMessage().getBody(), Map.class);
        Assertions.assertEquals("nut", body.get("ape"));
    }

}
