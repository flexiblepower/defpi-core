package org.flexiblepower.rest;

import java.io.IOException;
import java.net.URI;

import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.model.User;
import org.flexiblepower.orchestrator.MongoDbConnector;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class.
 *
 */
public class Main {

    // Base URI the Grizzly HTTP server will listen on
    public static final String BASE_URI = "http://0.0.0.0:8080/";
    final static Logger logger = LoggerFactory.getLogger(Main.class);

    /**
     * Starts HTTP server exposing JAX-RS resources defined in this application.
     */
    public static void startServer() {
        final ResourceConfig rc = ResourceConfig.forApplication(new OrchestratorApplication());
        JettyHttpContainerFactory.createServer(URI.create(Main.BASE_URI), rc);
    }

    /**
     * Main method.
     *
     * @param args
     * @throws AuthorizationException
     * @throws IOException
     */
    public static void main(final String[] args) throws AuthorizationException {
        final User admin = new User("admin", "admin");
        admin.setAdmin(true);
        final MongoDbConnector db = new MongoDbConnector();
        db.setApplicationUser(admin);
        db.insertUser(admin);

        Main.startServer();
        Main.logger.info(
                String.format("Jersey app started with WADL available at " + "%sapplication.wadl", Main.BASE_URI));
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
