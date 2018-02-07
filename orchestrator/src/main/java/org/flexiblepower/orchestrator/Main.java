/**
 * File Main.java
 *
 * Copyright 2017 FAN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.flexiblepower.orchestrator;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.UUID;

import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jetty.server.Server;
import org.flexiblepower.connectors.MongoDbConnector;
import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.model.User;
import org.flexiblepower.orchestrator.pendingchange.PendingChangeManager;
import org.flexiblepower.rest.OrchestratorApplication;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import lombok.extern.slf4j.Slf4j;

import io.swagger.models.Scheme;

/**
 * Main class.
 *
 */
@Slf4j
public class Main {

    // Base URI the HTTP server will listen on
    public static final String URI_SCHEME = Scheme.HTTP.name();
    public static final String URI_HOST = "localhost";
    public static final int URI_PORT = 8080;
    public static final String URI_PATH = "";

    private static final String ROOT_USER = "admin";
    private static final String ROOT_PASSWORD = "admin";

    /**
     * Starts HTTP server exposing JAX-RS resources defined in this application.
     *
     * @throws UnknownHostException
     * @throws URISyntaxException
     */
    public static Server startServer() throws UnknownHostException, URISyntaxException {
        final URI publishURI = new URIBuilder().setScheme(Main.URI_SCHEME)
                .setHost(Main.URI_HOST)
                .setPort(Main.URI_PORT)
                .setPath(Main.URI_PATH)
                .build();
        Main.log.info("Jersey app started with WADL available at {}/application.wadl", publishURI);
        final ResourceConfig rc = ResourceConfig.forApplication(new OrchestratorApplication(publishURI));

        return JettyHttpContainerFactory.createServer(publishURI, rc);
    }

    /**
     *
     */
    private static void ensureAdminUserExists() {
        Main.log.trace("Ensuring user with name {} exists", Main.ROOT_USER);
        final MongoDbConnector db = MongoDbConnector.getInstance();
        if (db.getUser(Main.ROOT_USER, Main.ROOT_PASSWORD) == null) {
            final User root = new User(Main.ROOT_USER, Main.ROOT_PASSWORD);
            root.setAuthenticationToken(UUID.randomUUID().toString());
            // root.setPasswordHash();
            root.setAdmin(true);

            UserManager.getInstance().saveUser(root);
        }
    }

    /**
     * Main method.
     *
     * @param args
     * @throws AuthorizationException
     * @throws URISyntaxException
     * @throws UnknownHostException
     */
    public static void
            main(final String[] args) throws AuthorizationException, UnknownHostException, URISyntaxException {
        Main.ensureAdminUserExists();
        Main.startServer();
        PendingChangeManager.getInstance(); // make sure it starts
    }
}
