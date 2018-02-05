/**
 * File ConnectionApi.java
 *
 * Copyright 2017 FAN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.flexiblepower.api;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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

/**
 * This is the interface of the orchestrator Connections API.
 *
 * @version 0.1
 * @since Dec 6, 2017
 */
@Api("Connection")
@Path("connection")
public interface ConnectionApi {

    /**
     * Error message to display if the connection is not found
     */
    static final String CONNECTION_NOT_FOUND_MESSAGE = "Connection not found";
    /**
     * Error message to display if the interface is not found
     */
    static final String INTERFACE_NOT_FOUND_MESSAGE = "Interface to bind to was not found";

    /**
     * List all existing connections for the current logged in user. By design if the user is an administrator, all
     * connections are returned.
     *
     * @param page the current page to view (defaults to 1)
     * @param perPage the amount of connections to view per page (defaults to 1000)
     * @param sortDir the direction to sort the connections (defaults to "ASC")
     * @param sortField the field to sort the pending changes on (defaults to "id")
     * @param filters a list of filters in JSON notation (defaults to "{}")
     * @return a list of connections that belong to the current user
     * @throws AuthorizationException if the user is not authenticated at all
     */
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
    public List<Connection> listConnections(@QueryParam("_page") @DefaultValue("1") int page,
            @QueryParam("_perPage") @DefaultValue(OrchestratorApi.DEFAULT_ITEMS_PER_PAGE) int perPage,
            @QueryParam("_sortDir") @DefaultValue("ASC") String sortDir,
            @QueryParam("_sortField") @DefaultValue("id") String sortField,
            @QueryParam("_filters") @DefaultValue("{}") String filters) throws AuthorizationException;

    /**
     * Get the connection with the specified Id
     *
     * @param id The id of the connection to look up
     * @return Connection with the specified Id
     * @throws AuthorizationException if the user is not authorized to get this connection
     * @throws ProcessNotFoundException When no process with the provided ObjectId is found
     * @throws InvalidObjectIdException When the provided id is not a valid ObjectId
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
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
            throws AuthorizationException,
            ProcessNotFoundException,
            InvalidObjectIdException;

    /**
     * Creates a new connection between two processes
     *
     * @param connection The new connection to insert
     * @return The id of the new connection
     * @throws AuthorizationException if the user is not authenticated at all
     * @throws NotFoundException
     * @throws ConnectionException
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
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
            throws AuthorizationException,
            NotFoundException,
            ConnectionException;

    /**
     * Removes an existing connection between two processes
     *
     * @param connectionId The id of the connection to remove
     * @throws AuthorizationException if the user is not authenticated at all
     * @throws InvalidObjectIdException
     * @throws NotFoundException
     */
    @DELETE
    @Path("{connectionId}")
    @Consumes(MediaType.TEXT_PLAIN)
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
                      required = true) @PathParam("connectionId") final String connectionId)
            throws AuthorizationException,
            InvalidObjectIdException,
            NotFoundException;
}
