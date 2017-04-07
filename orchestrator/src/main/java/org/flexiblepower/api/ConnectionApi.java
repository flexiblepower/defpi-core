package org.flexiblepower.api;

import java.util.Collection;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.flexiblepower.model.Connection;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;

@Api("Connection")
@Path("connection")
@Produces(MediaType.APPLICATION_JSON)
public interface ConnectionApi {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Connection> listConnections() throws Exception;

    @POST
    @ApiOperation(value = "Create a new connection",
                  notes = "",
                  authorizations = {@Authorization(value = "AdminSecurity")})
    @Consumes(MediaType.APPLICATION_JSON)
    public String newConnection(final Connection connection) throws Exception;

    @DELETE
    @Path("{id}")
    public void deleteConnection(@PathParam("id") final String id) throws Exception;
}
