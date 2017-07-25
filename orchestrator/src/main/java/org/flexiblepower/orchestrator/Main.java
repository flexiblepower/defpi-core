package org.flexiblepower.orchestrator;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import org.apache.http.client.utils.URIBuilder;
import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.model.User;
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
    private static final String URI_HOST = "192.168.239.128";
    private static final int URI_PORT = 8080;
    private static final String URI_PATH = "";

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
        // final Services services = new Services(null);
        // while (true) {
        // try {
        // // services.syncServices();
        // // Swarm.syncHosts();
        // Thread.sleep(10000);
        // } catch (final InterruptedException e) {
        // e.printStackTrace();
        // } catch (final Exception e) {
        // e.printStackTrace();
        // }
        // }
    }
}
