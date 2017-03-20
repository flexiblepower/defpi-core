package org.flexiblepower.rest;

import java.util.Collection;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.flexiblepower.gson.InitGson;
import org.flexiblepower.model.Connection;
import org.flexiblepower.model.User;
import org.flexiblepower.orchestrator.MongoDbConnector;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import lombok.extern.slf4j.Slf4j;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;

@Path("Connections")
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
            throw new ApiException(e);
        }
    }

    @POST
    @ApiOperation(value = "Create a new user",
                  notes = "",
                  response = User.class,
                  authorizations = {@Authorization(value = "AdminSecurity")},
                  tags = {"User",})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response newConnection(@javax.ws.rs.core.Context final HttpHeaders httpHeaders, final String json) {
        ConnectionApi.log.info("newConnection(): " + json);
        // if (this.initUser(httpHeaders)) {
        try {
            final Gson gson = InitGson.create();
            final Connection Connection = gson.fromJson(json, Connection.class);
            return Response.status(this.Connections.newConnection(Connection)).build();
        } catch (final JsonSyntaxException e) {
            ConnectionApi.logger.info("Parse exception: " + e);
            return Response.status(Status.BAD_REQUEST).build();
        }
        // }
        // return Response.status(Status.UNAUTHORIZED).build();
    }
    //
    // @DELETE
    // @Path("{id}")
    // public Response deleteConnection(@javax.ws.rs.core.Context final HttpHeaders httpHeaders,
    // @PathParam("id") final String id) {
    // ConnectionApi.logger.info("deleteConnection(): " + id);
    // if (this.initUser(httpHeaders)) {
    // return Response.status(this.Connections.deleteConnection(id)).build();
    // }
    // return Response.status(Status.UNAUTHORIZED).build();
    // }
}
