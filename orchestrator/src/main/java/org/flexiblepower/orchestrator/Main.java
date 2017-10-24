package org.flexiblepower.orchestrator;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.UUID;

import org.apache.http.client.utils.URIBuilder;
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
    public static final String URI_HOST = "defpi.hesilab.nl";
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
    public static void startServer() throws UnknownHostException, URISyntaxException {
        final URI publishURI = new URIBuilder().setScheme(Main.URI_SCHEME)
                .setHost(Main.URI_HOST)
                .setPort(Main.URI_PORT)
                .setPath(Main.URI_PATH)
                .build();
        Main.log.info("Jersey app started with WADL available at {}/application.wadl", publishURI);
        final ResourceConfig rc = ResourceConfig.forApplication(new OrchestratorApplication(publishURI));

        JettyHttpContainerFactory.createServer(publishURI, rc);
    }

    /**
     *
     */
    private static void ensureAdminUserExists() {
        final MongoDbConnector db = MongoDbConnector.getInstance();
        if (db.getUser(Main.ROOT_USER, Main.ROOT_PASSWORD) == null) {
            final User root = new User(Main.ROOT_USER, Main.ROOT_PASSWORD);
            root.setAuthenticationToken(UUID.randomUUID().toString());
            root.setPasswordHash();
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
