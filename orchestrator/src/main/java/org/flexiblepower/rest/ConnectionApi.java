package org.flexiblepower.rest;

import java.util.Collection;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.flexiblepower.exceptions.ApiException;
import org.flexiblepower.model.Connection;
import org.flexiblepower.orchestrator.MongoDbConnector;

import lombok.extern.slf4j.Slf4j;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;

@Path("connections")
@Slf4j
public class ConnectionApi {

    private final MongoDbConnector db = new MongoDbConnector();

    private void initUser(final HttpHeaders httpHeaders) {
        final String username = httpHeaders.getHeaderString("username");
        final String password = httpHeaders.getHeaderString("password");
        this.db.setApplicationUser(this.db.getUser(username, password));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Connection> listConnections(@javax.ws.rs.core.Context final HttpHeaders httpHeaders) {
        this.initUser(httpHeaders);
        try {
            return this.db.getConnections();
        } catch (final Exception e) {
            e.printStackTrace();
            throw new ApiException(e);
        }
    }

    @POST
    @ApiOperation(value = "Create a new connection",
                  notes = "",
                  authorizations = {@Authorization(value = "AdminSecurity")},
                  tags = {"User"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response newConnection(@javax.ws.rs.core.Context final HttpHeaders httpHeaders,
            final Connection connection) {
        ConnectionApi.log.info("newConnection(): " + connection);
        // if (this.initUser(httpHeaders)) {
        this.db.insertConnection(connection);
        return Response.ok().build();
    }

    @DELETE
    @Path("{id}")
    public Response deleteConnection(@javax.ws.rs.core.Context final HttpHeaders httpHeaders,
            @PathParam("id") final String id) {
        ConnectionApi.logger.info("deleteConnection(): " + id);
        if (this.initUser(httpHeaders)) {
            return Response.status(this.Connections.deleteConnection(id)).build();
        }
        return Response.status(Status.UNAUTHORIZED).build();
    }
}
