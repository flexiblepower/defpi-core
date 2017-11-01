package org.flexiblepower.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.flexiblepower.connectors.DockerConnector;

@Path("diag")
@SuppressWarnings("static-method")
public class UtilApi {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getVersion() {
        return DockerConnector.getInstance().getContainerInfo();
    }

}
