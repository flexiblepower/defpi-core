/**
 * File RamlIntegrationTest.java
 *
 * Copyright 2019 FAN
 */
package org.flexiblepower.raml;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.flexiblepower.proto.RamlProto.RamlRequest;
import org.flexiblepower.proto.RamlProto.RamlResponse;
import org.flexiblepower.raml.client.RamlProxyClient;
import org.flexiblepower.raml.client.TestClientConnectionHandler;
import org.flexiblepower.raml.client.TestConnection;
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
@SuppressWarnings({"javadoc", "static-method"})
public class RamlIntegrationTest {

    static final TestConnection testConnection = new TestConnection();
    static final ExecutorService executor = Executors.newFixedThreadPool(2);
    static final Server server = new Server();
    static final Client client = new Client();

    @Test
    public void runSimpleTest() {
        Assertions.assertEquals("Hello world!", RamlIntegrationTest.client.getExample().getExampleText());
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
        final Example example = RamlProxyClient.generateClient(Example.class, this.handler);

        public volatile boolean running = true;

        public Example getExample() {
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
