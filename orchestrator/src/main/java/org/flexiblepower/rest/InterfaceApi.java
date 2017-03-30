package org.flexiblepower.rest;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.flexiblepower.model.Interface;
import org.flexiblepower.orchestrator.RegistryConnector;

import io.swagger.annotations.Api;

@Path("interface")
@Api("Interface")
public class InterfaceApi extends BaseApi {

    private final RegistryConnector registryConnector = new RegistryConnector();

    protected InterfaceApi(@Context final HttpHeaders httpHeaders) {
        super(httpHeaders);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Interface> listProtos(@Context final HttpHeaders httpHeaders) {
        // this.initUser(httpHeaders);
        return this.registryConnector.getInterfaces();
    }

    @GET
    @Path("{sha256}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response download(@Context final HttpHeaders httpHeaders, @PathParam("sha256") final String sha256) {
        // this.initUser(httpHeaders);
        final Interface itf = this.registryConnector.getInterface(sha256);
        if (itf == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        final ResponseBuilder builder = Response.ok(itf);
        builder.header("Content-Disposition", "attachment; filename=\"message.proto\"");
        return builder.build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public String newInterface(@Context final HttpHeaders httpHeaders, final Interface itf) {
        // this.initUser(httpHeaders);
        return this.registryConnector.addInterface(itf);
    }

    @DELETE
    @Path("{sha256}")
    public void deleteInterface(@Context final HttpHeaders httpHeaders, @PathParam("sha256") final String sha256) {
        // this.initUser(httpHeaders);
        this.registryConnector.deleteInterface(sha256);
    }
}
