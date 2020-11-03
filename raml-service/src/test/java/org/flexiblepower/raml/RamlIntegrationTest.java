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

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.flexiblepower.proto.RamlProto.RamlRequest;
import org.flexiblepower.proto.RamlProto.RamlResponse;
import org.flexiblepower.raml.client.RamlProxyClient;
import org.flexiblepower.raml.client.TestClientConnectionHandler;
import org.flexiblepower.raml.client.TestConnection;
import org.flexiblepower.raml.example.Humans;
import org.flexiblepower.raml.example.model.Gender;
import org.flexiblepower.raml.example.model.Human;
import org.flexiblepower.raml.example.model.HumanImpl;
import org.flexiblepower.raml.example.model.Person;
import org.flexiblepower.raml.example.model.PersonImpl;
import org.flexiblepower.raml.server.TestServerConnectionHandler;
import org.flexiblepower.service.TestConnectionManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * RamlIntegrationTest
 *
 * @version 0.1
 * @since Aug 27, 2019
 */
// @Timeout(value = 10, unit = TimeUnit.SECONDS)
@SuppressWarnings({"javadoc", "static-method"})
public class RamlIntegrationTest {

    static final TestConnection testConnection = new TestConnection();
    static final ExecutorService executor = Executors.newCachedThreadPool();
    static final Server server = new Server();
    static final Client client = new Client();
    private static final TypeReference<List<Human>> listOfHumans = new TypeReference<List<Human>>() {
        // Just for parsing
    };

    @Test
    public void runSimpleTest() {
        Assertions.assertEquals("Hello world!", RamlIntegrationTest.client.getExample().getExampleText());
    }

    @Test
    public void runSubTypeTest() {
        System.out.println(RamlIntegrationTest.client.getHumans().getHumansPersonById("henk", "doctor"));
    }

    @Test
    public void runQueryTest() {
        Assertions.assertEquals("Hello Wrapper!", RamlIntegrationTest.client.getExample().getPersonalText("Wrapper"));
    }

    @Test
    public void runPathTest() {
        Assertions.assertEquals("Hello world!\nHello world!\n",
                RamlIntegrationTest.client.getExample().getPersonalText(2));
    }

    @Test
    public void runComplexTest() {
        Assertions.assertEquals(187f,
                RamlIntegrationTest.client.getExample()
                        .setStuff(100, "waarde", 25.0, Collections.singletonMap("waarde", "82")));
    }

    @Test
    public void runErrorTest() throws Exception {
        Assertions.assertThrows(NullPointerException.class,
                () -> RamlIntegrationTest.client.getExample()
                        .setStuff(100, "waarde", 25.0, Collections.singletonMap("waarde", null)));
        Assertions.assertThrows(NumberFormatException.class,
                () -> RamlIntegrationTest.client.getExample()
                        .setStuff(100, "waarde", 25.0, Collections.singletonMap("waarde", "zeven")));
    }

    @Test
    public void runPutGeneratedTest() {
        final Human h = new HumanImpl();
        h.setActualGender(Gender.MALE);
        h.setDateOfBirth(new Date());
        RamlIntegrationTest.client.getHumans().putHumansById("henk", h);
    }

    @Test
    public void runSimpleGeneratedTest() {
        RamlProxyClient.registerTypeReference("/humans/all", RamlIntegrationTest.listOfHumans);
        System.out.println(Collections.singletonMap("generateBuilders", true));
        final List<Human> list = RamlIntegrationTest.client.getHumans().getHumansAll();
        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals("person", list.get(0).getHumanType());
        Assertions.assertNotNull(list.get(0).getDateOfBirth());
    }

    @Test
    public void runQueryGeneratedTest() {
        final Human somebody = RamlIntegrationTest.client.getHumans().getHumansById("piet", null);
        Assertions.assertEquals("person", somebody.getHumanType());
        Assertions.assertNotNull(somebody.getDateOfBirth());
    }

    @Test
    public void runPathGeneratedTest() throws JsonProcessingException {
        final List<Human> list = RamlIntegrationTest.client.getHumans().getHumans("henk");
        final List<Human> humans = RamlProxyClient.readGenericEntity(list, RamlIntegrationTest.listOfHumans);
        Assertions.assertEquals(1, humans.size());
        Assertions.assertEquals("person", humans.get(0).getHumanType());
        Assertions.assertNotNull(humans.get(0).getDateOfBirth());
        Assertions.assertEquals("henk", ((Person) humans.get(0)).getName());
    }

    @Test
    public void runErrorGeneratedTest() throws Exception {
        final PersonImpl npePerson = new PersonImpl();
        Assertions.assertThrows(NullPointerException.class,
                () -> RamlIntegrationTest.client.getHumans().putHumansById("21", npePerson));
        npePerson.setName("zeven");
        Assertions.assertThrows(NumberFormatException.class,
                () -> RamlIntegrationTest.client.getHumans().putHumansById("21", npePerson));
    }

    @AfterAll
    public static void stop() {
        RamlIntegrationTest.server.running = false;
        RamlIntegrationTest.client.running = false;
        RamlIntegrationTest.executor.shutdownNow();
    }

    public static class Client {

        final TestClientConnectionHandler handler = TestConnectionManager
                .getClientHandler(RamlIntegrationTest.testConnection);
        final Humans humans = RamlProxyClient.generateClient(Humans.class, this.handler);
        final Example example = RamlProxyClient.generateClient(Example.class, this.handler);

        public volatile boolean running = true;

        public Example getExample() {
            return this.example;
        }

        public Humans getHumans() {
            return this.humans;
        }

        Client() {
            RamlIntegrationTest.executor.submit(this::addResponsesToHandler);
        }

        // Normally this is done by the TCP Connection
        void addResponsesToHandler() {
            while (this.running) {
                if (RamlIntegrationTest.testConnection.contains(RamlResponse.class)) {
                    this.handler.handleRamlResponse((RamlResponse) RamlIntegrationTest.testConnection.pop());
                } else {
                    try {
                        Thread.sleep(50);
                    } catch (final InterruptedException e) {
                        break;
                    }
                }
            }
        }
    }

    public static class Server {

        final TestServerConnectionHandler handler = TestConnectionManager
                .getServerHandler(RamlIntegrationTest.testConnection);
        public volatile boolean running = true;

        Server() {
            RamlIntegrationTest.executor.submit(this::addRequestsToHandler);
        }

        // Normally this is done by the TCP Connection
        void addRequestsToHandler() {
            while (this.running) {
                if (RamlIntegrationTest.testConnection.contains(RamlRequest.class)) {
                    this.handler.handleRamlRequest((RamlRequest) RamlIntegrationTest.testConnection.pop());
                } else {
                    try {
                        Thread.sleep(50);
                    } catch (final InterruptedException e) {
                        break;
                    }
                }
            }
        }

    }

}
