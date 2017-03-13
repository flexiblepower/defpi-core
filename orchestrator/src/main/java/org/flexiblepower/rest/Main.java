package org.flexiblepower.rest;

import java.io.IOException;
import java.net.URI;

import org.flexiblepower.orchestrator.CORSResponseFilter;
import org.flexiblepower.orchestrator.Services;
import org.flexiblepower.orchestrator.Swarm;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
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
     * @return Grizzly HTTP server.
     */
    public static HttpServer startServer() {
        final ResourceConfig rc = new ResourceConfig().packages("org.flexiblepower.rest");
        rc.register(CORSResponseFilter.class);
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
    }

    /**
     * Main method.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        startServer();
        logger.info(String.format("Jersey app started with WADL available at "
                + "%sapplication.wadl", BASE_URI));
    	Services services = new Services(null);
        while(true){
        	try {
    			services.syncServices();
        		Swarm.syncHosts();
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (Exception e){
				e.printStackTrace();
			}
        }
    }
}

