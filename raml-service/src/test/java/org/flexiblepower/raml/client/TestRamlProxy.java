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

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.flexiblepower.proto.RamlProto.RamlRequest;
import org.flexiblepower.proto.RamlProto.RamlRequest.Method;
import org.flexiblepower.raml.example.Humans;
import org.flexiblepower.raml.example.model.Arm;
import org.flexiblepower.raml.example.model.ArmImpl;
import org.flexiblepower.raml.example.model.Gender;
import org.flexiblepower.raml.example.model.Human;
import org.flexiblepower.raml.example.model.HumanImpl;
import org.flexiblepower.raml.example.model.Leg;
import org.flexiblepower.raml.example.model.LegImpl;
import org.flexiblepower.service.ConnectionHandler;
import org.flexiblepower.service.TestConnectionManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

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

    final static TestConnection connection = new TestConnection();
    final ObjectMapper mapper = new ObjectMapper();
    final ExecutorService executor = Executors.newCachedThreadPool();

    static Humans ex;

    @BeforeAll
    public static void init() {
        final ConnectionHandler handler = TestConnectionManager.getClientHandler(TestRamlProxy.connection);
        TestRamlProxy.ex = RamlProxyClient.generateClient(Humans.class, handler);
    }

    @Test
    @Timeout(1)
    public void runTest() throws InterruptedException {
        // Have to run it asynchronously because it will block forever until it receives a response
        final Future<?> f = this.executor.submit(TestRamlProxy.ex::getHumansAll);
        while (!TestRamlProxy.connection.contains(RamlRequest.class)) {
            Thread.sleep(10);
        }
        f.cancel(true);

        final RamlRequest msg = (RamlRequest) TestRamlProxy.connection.pop();
        Assertions.assertEquals("/example", msg.getUri());
        Assertions.assertEquals(Method.GET, msg.getMethod());
        Assertions.assertFalse(msg.hasBody());
    }

    @Test
    @Timeout(1)
    public void runQueryParamTest() throws InterruptedException {
        // Have to run it asynchronously because it will block forever until it receives a response
        final Future<?> f = this.executor.submit(() -> TestRamlProxy.ex.getHumans("male"));
        while (!TestRamlProxy.connection.contains(RamlRequest.class)) {
            Thread.sleep(10);
        }
        f.cancel(true);

        final RamlRequest msg = (RamlRequest) TestRamlProxy.connection.pop();
        Assertions.assertEquals("/example?name=Harry", msg.getUri());
        Assertions.assertEquals(Method.GET, msg.getMethod());
        Assertions.assertFalse(msg.hasBody());
    }

    @Test
    @Timeout(1)
    public void runPathParamTest() throws InterruptedException {
        // Have to run it asynchronously because it will block forever until it receives a response
        final Future<?> f = this.executor.submit(() -> TestRamlProxy.ex.getHumansById("Harry", "male"));
        while (!TestRamlProxy.connection.contains(RamlRequest.class)) {
            Thread.sleep(10);
        }
        f.cancel(true);

        final RamlRequest msg = (RamlRequest) TestRamlProxy.connection.pop();
        Assertions.assertEquals("/example/5", msg.getUri());
        Assertions.assertEquals(Method.GET, msg.getMethod());
        Assertions.assertFalse(msg.hasBody());
    }

    @Test
    @Timeout(1)
    public void runComplexTest() throws InterruptedException, JsonMappingException, JsonProcessingException {
        // Have to run it asynchronously because it will block forever until it receives a response
        final Human henk = new HumanImpl();
        henk.setDateOfBirth(java.util.Date.from(Instant.EPOCH)); // Such a coincidence
        henk.setActualGender(Gender.MALE);

        final Leg leg = new LegImpl();
        leg.setToes(5);
        final Arm arm = new ArmImpl();
        arm.setFingers(4);

        henk.setLimbs(Arrays.asList(leg, leg, arm, arm));

        final Future<?> f = this.executor.submit(() -> TestRamlProxy.ex.putHumansById("Henk", henk));
        while (!TestRamlProxy.connection.contains(RamlRequest.class)) {
            Thread.sleep(10);
        }
        f.cancel(true);

        final RamlRequest msg = (RamlRequest) TestRamlProxy.connection.pop();
        final String messageUri = msg.getUri();
        Assertions.assertEquals(55, messageUri.length());
        Assertions.assertEquals("/example/complicated/187?", messageUri.substring(0, 25));
        Assertions.assertTrue(messageUri.contains("test=3.141592653589793"));
        Assertions.assertTrue(messageUri.contains("q=query"));
        Assertions.assertTrue(messageUri.contains("&"));
        Assertions.assertEquals(Method.POST, msg.getMethod());
        Assertions.assertTrue(msg.hasBody());

        @SuppressWarnings("unchecked")
        final Map<String, String> body = this.mapper.readValue(msg.getBody(), Map.class);
        Assertions.assertEquals("nut", body.get("ape"));
    }

}
