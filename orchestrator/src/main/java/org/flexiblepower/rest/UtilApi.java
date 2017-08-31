package org.flexiblepower.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.flexiblepower.connectors.DockerConnector;

@Path("diag")
@SuppressWarnings("static-method")
public class UtilApi extends BaseApi {

    /**
     * @param httpHeaders
     */
    protected UtilApi(@Context final HttpHeaders httpHeaders) {
        super(httpHeaders);
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getVersion() {
        return DockerConnector.getInstance().getContainerInfo();
    }

}
