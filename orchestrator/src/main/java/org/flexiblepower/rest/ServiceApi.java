package org.flexiblepower.rest;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.flexiblepower.model.Service;
import org.flexiblepower.orchestrator.RegistryConnector;

import io.swagger.annotations.Api;

@Path("service")
@Api("Service")
@Produces(MediaType.APPLICATION_JSON)
public class ServiceApi extends BaseApi {

    protected final RegistryConnector registryConnector = new RegistryConnector();

    protected ServiceApi(@Context final HttpHeaders httpHeaders) {
        super(httpHeaders);
    }

    @GET
    public List<String> listServices(@Context final HttpHeaders httpHeaders) {
        // this.initUser(httpHeaders);
        return this.registryConnector.getServices();
    }

    @GET
    @Path("{image}/{tag}")
    public Service listService(@Context final HttpHeaders httpHeaders,
            @PathParam("image") final String imageName,
            @PathParam("tag") final String tag) {
        // this.initUser(httpHeaders);
        return this.registryConnector.getService(imageName, tag);
    }

    @DELETE
    @Path("{image}/{tag}")
    public void deleteService(@Context final HttpHeaders httpHeaders,
            @PathParam("image") final String image,
            @PathParam("tag") final String tag) {
        // this.initUser(httpHeaders);
        this.registryConnector.deleteService(image, tag);
    }

}
