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
package org.flexiblepower.orchestrator;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jetty.server.Server;
import org.flexiblepower.connectors.DockerConnector;
import org.flexiblepower.connectors.MongoDbConnector;
import org.flexiblepower.connectors.ProcessConnector;
import org.flexiblepower.connectors.RegistryConnector;
import org.flexiblepower.model.User;
import org.flexiblepower.orchestrator.pendingchange.PendingChangeManager;
import org.flexiblepower.process.ProcessManager;
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
    private static final String URI_SCHEME = Scheme.HTTP.name();
    private static final String URI_HOST = "localhost";
    /**
     * The port where the orchestrator listens for REST calls
     */
    public static final int URI_PORT = 8080;
    private static final String URI_PATH = "";

    private static final String ROOT_USER = "admin";
    private static final String ROOT_PASSWORD = "admin";

    /**
     * Starts HTTP server exposing JAX-RS resources defined in this application.
     *
     * @return The HTTP server object that was started
     * @throws URISyntaxException If the created publish URI is invalid.
     */
    public static Server startServer() throws URISyntaxException {
        final URI publishURI = new URIBuilder().setScheme(Main.URI_SCHEME)
                .setHost(Main.URI_HOST)
                .setPort(Main.URI_PORT)
                .setPath(Main.URI_PATH)
                .build();
        Main.log.info("Jersey app started with WADL available at {}/application.wadl", publishURI);
        final ResourceConfig rc = ResourceConfig.forApplication(new OrchestratorApplication(publishURI));

        return JettyHttpContainerFactory.createServer(publishURI, rc);
    }

    private static void ensureAdminUserExists() {
        Main.log.trace("Ensuring user with name {} exists", Main.ROOT_USER);
        // if (db.getUser(Main.ROOT_USER, Main.ROOT_PASSWORD) == null) {
        if (UserManager.getInstance().getUser(Main.ROOT_USER) == null) {
            final User root = new User(Main.ROOT_USER, Main.ROOT_PASSWORD);
            // root.setPasswordHash();
            root.setAdmin(true);

            UserManager.getInstance().saveUser(root);
        }
    }

    /**
     * Main method.
     *
     * @param args Command line arguments (ignored)
     * @throws URISyntaxException When the dynamically built URI is invalid
     */
    public static void main(final String[] args) throws URISyntaxException {
        Main.ensureAdminUserExists();
        Main.startServer();
        PendingChangeManager.getInstance(); // make sure it starts

        // Make sure all instances are initialized so we don't have to synchronize them
        DockerConnector.getInstance();
        MongoDbConnector.getInstance();
        ProcessConnector.getInstance();
        RegistryConnector.getInstance();

        NodeManager.getInstance();
        ServiceManager.getInstance();
        UserManager.getInstance();
        ProcessManager.getInstance();

        DockerConnector.getInstance().ensureAllUserNetsAttached();
    }
}
