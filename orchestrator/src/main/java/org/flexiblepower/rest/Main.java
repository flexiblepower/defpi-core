package org.flexiblepower.rest;

import java.io.IOException;
import java.net.URI;

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
    public static final String BASE_URI = "http://0.0.0.0:80/";
    final static Logger logger = LoggerFactory.getLogger(Main.class);

    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
     *
     * @return Grizzly HTTP server.
     */
    public static void startServer() {
        final ResourceConfig rc = new ResourceConfig().packages("org.flexiblepower");
        rc.register(CORSResponseFilter.class);
        JettyHttpContainerFactory.createServer(URI.create(Main.BASE_URI), rc);
    }

    /**
     * Main method.
     *
     * @param args
     * @throws IOException
     */
    public static void main(final String[] args) {
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
