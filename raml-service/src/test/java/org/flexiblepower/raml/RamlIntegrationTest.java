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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ws.rs.core.GenericType;

import org.flexiblepower.proto.RamlProto.RamlRequest;
import org.flexiblepower.proto.RamlProto.RamlResponse;
import org.flexiblepower.raml.client.RamlProxyClient;
import org.flexiblepower.raml.client.TestClientConnectionHandler;
import org.flexiblepower.raml.client.TestConnection;
import org.flexiblepower.raml.example.Humans;
import org.flexiblepower.raml.example.Humans.GetHumansAllResponse;
import org.flexiblepower.raml.example.Humans.GetHumansByIdResponse;
import org.flexiblepower.raml.example.model.Human;
import org.flexiblepower.raml.server.TestServerConnectionHandler;
import org.flexiblepower.service.TestConnectionManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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

    @Test
    public void runSimpleTest() {
        final GetHumansAllResponse r = RamlIntegrationTest.client.getExample().getHumansAll();
        final List<Human> list = r.readEntity(new GenericType<List<Human>>() {
            // This is okay
        });
        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals("Person", list.get(0).getHumanType());
        Assertions.assertNotNull(list.get(0).getDateOfBirth());
    }

    @Test
    public void runQueryTest() {
        final GetHumansByIdResponse r = RamlIntegrationTest.client.getExample().getHumansById("piet", null);
        final Human somebody = r.readEntity(Human.class);
        Assertions.assertEquals("Person", somebody.getHumanType());
        Assertions.assertNotNull(somebody.getDateOfBirth());
    }
    //
    // @Test
    // public void runPathTest() {
    // Assertions.assertEquals("Hello world!\nHello world!\n",
    // RamlIntegrationTest.client.getExample().getPersonalText(2));
    // }
    //
    // @Test
    // public void runComplexTest() {
    // Assertions.assertEquals(187f,
    // RamlIntegrationTest.client.getExample()
    // .setStuff(100, "waarde", 25.0, Collections.singletonMap("waarde", "82")));
    // }
    //
    // @Test
    // public void runErrorTest() throws Exception {
    // Assertions.assertThrows(NullPointerException.class,
    // () -> RamlIntegrationTest.client.getExample()
    // .setStuff(100, "waarde", 25.0, Collections.singletonMap("waarde", null)));
    // Assertions.assertThrows(NumberFormatException.class,
    // () -> RamlIntegrationTest.client.getExample()
    // .setStuff(100, "waarde", 25.0, Collections.singletonMap("waarde", "zeven")));
    // }

    @AfterAll
    public static void stop() {
        RamlIntegrationTest.server.running = false;
        RamlIntegrationTest.client.running = false;
        RamlIntegrationTest.executor.shutdownNow();
    }

    public static class Client {

        final TestClientConnectionHandler handler = TestConnectionManager
                .getClientHandler(RamlIntegrationTest.testConnection);
        final Humans example = RamlProxyClient.generateClient(Humans.class, this.handler);

        public volatile boolean running = true;

        public Humans getExample() {
            return this.example;
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
