package org.flexiblepower.api;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.ConnectionException;
import org.flexiblepower.exceptions.InvalidObjectIdException;
import org.flexiblepower.exceptions.NotFoundException;
import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.model.Connection;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Api("Connection")
@Path("connection")
public interface ConnectionApi {

    static final String CONNECTION_NOT_FOUND_MESSAGE = "Connection not found";
    static final String INTERFACE_NOT_FOUND_MESSAGE = "Interface to bind to was not found";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "listConnections",
                  value = "List connections",
                  notes = "List all existing connections",
                  authorizations = {@Authorization(value = OrchestratorApi.USER_AUTHENTICATION)})
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                         message = "An array of Connections",
                         response = Connection.class,
                         responseContainer = "List"),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public List<Connection> listConnections() throws AuthorizationException;

    @GET
    @Path("{connectionId}")
    @ApiOperation(nickname = "getConnection",
                  value = "Get connection data",
                  notes = "Get the connection with the specified Id",
                  authorizations = {@Authorization(value = OrchestratorApi.USER_AUTHENTICATION)})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Connection with the specified Id", response = Connection.class),
            @ApiResponse(code = 400, message = InvalidObjectIdException.INVALID_OBJECT_ID_MESSAGE),
            @ApiResponse(code = 404, message = ProcessApi.PROCESS_NOT_FOUND_MESSAGE),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public Connection getConnection(
            @ApiParam(name = "connectionId",
                      value = "The id of the connection",
                      required = true) @PathParam("connectionId") final String id)
            throws AuthorizationException, ProcessNotFoundException, InvalidObjectIdException;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "newConnection",
                  value = "Create a new connection",
                  notes = "Creates a new connection between two processes",
                  authorizations = {@Authorization(value = OrchestratorApi.USER_AUTHENTICATION)})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The id of the new connection", response = Connection.class),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE),
            @ApiResponse(code = 404, message = ConnectionApi.INTERFACE_NOT_FOUND_MESSAGE)})
    public Connection newConnection(
            @ApiParam(name = "connection",
                      value = "The new connection to insert",
                      required = true) final Connection connection)
            throws AuthorizationException, NotFoundException, ConnectionException;

    @DELETE
    @Path("{id}")
    @ApiOperation(nickname = "deleteConnection",
                  value = "Remove a connection",
                  notes = "Removes an existing connection between two processes",
                  code = 204,
                  authorizations = {@Authorization(value = OrchestratorApi.USER_AUTHENTICATION)})
    @ApiResponses(value = {@ApiResponse(code = 204, message = "Connection deleted"),
            @ApiResponse(code = 400, message = InvalidObjectIdException.INVALID_OBJECT_ID_MESSAGE),
            @ApiResponse(code = 404, message = ConnectionApi.INTERFACE_NOT_FOUND_MESSAGE),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public void deleteConnection(
            @ApiParam(name = "connectionId",
                      value = "The id of the connection to remove",
                      required = true) @PathParam("id") final String id)
            throws AuthorizationException, InvalidObjectIdException, NotFoundException;
}
