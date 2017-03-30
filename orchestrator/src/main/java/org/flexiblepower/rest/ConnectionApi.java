package org.flexiblepower.rest;

import java.util.Collection;

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

import org.flexiblepower.exceptions.ApiException;
import org.flexiblepower.model.Connection;

import lombok.extern.slf4j.Slf4j;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;

@Path("connection")
@Slf4j
@Api("Connection")
public class ConnectionApi extends BaseApi {

    protected ConnectionApi(@Context final HttpHeaders httpHeaders) {
        super(httpHeaders);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Connection> listConnections(@Context final HttpHeaders httpHeaders) {
        try {
            // this.initUser(httpHeaders);
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
    public String newConnection(@Context final HttpHeaders httpHeaders, final Connection connection) {
        ConnectionApi.log.info("newConnection(): " + connection);
        try {
            // this.initUser(httpHeaders);
            return this.db.insertConnection(connection);
        } catch (final Exception e) {
            ConnectionApi.log.warn(e.getMessage(), e);
            throw new ApiException(e);
        }
    }

    @DELETE
    @Path("{id}")
    public void deleteConnection(@Context final HttpHeaders httpHeaders, @PathParam("id") final String id) {
        ConnectionApi.log.info("deleteConnection(): " + id);
        try {
            // this.initUser(httpHeaders);
            this.db.deleteConnection(id);
        } catch (final Exception e) {
            ConnectionApi.log.warn(e.getMessage(), e);
            throw new ApiException(e);
        }
    }
}
