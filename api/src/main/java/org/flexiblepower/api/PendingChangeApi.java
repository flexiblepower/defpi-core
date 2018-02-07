/**
 * File PendingChangeApi.java
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

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.InvalidObjectIdException;
import org.flexiblepower.exceptions.NotFoundException;
import org.flexiblepower.model.PendingChangeDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * PendingChangeApi
 *
 * @version 0.1
 * @since Apr 7, 2017
 */
@Api("PendingChange")
@Path("pendingchange")
public interface PendingChangeApi {

    /**
     * Delete the pending change with a known ObjectId. This will prevent that the pending change will be executed in
     * the future.
     *
     * @param pendingChangeId the ObjectId of the pending change to delete
     * @throws AuthorizationException When the user is not authorized to delete the pending change
     * @throws InvalidObjectIdException When the provided id is not a valid ObjectId
     * @throws NotFoundException When the pending change could not be found
     */
    @DELETE
    @Path("/{pendingchange_id}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "deletePendingChange",
                  value = "Delete PendingChange",
                  notes = "Delete the PendingChange with the provided Id",
                  code = 204,
                  authorizations = {@Authorization(value = OrchestratorApi.ADMIN_AUTHENTICATION)})
    @ApiResponses(value = {@ApiResponse(code = 204, message = "PendingChange deleted"),
            @ApiResponse(code = 400, message = InvalidObjectIdException.INVALID_OBJECT_ID_MESSAGE),
            @ApiResponse(code = 404, message = "PendingChange not found"),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public void deletePendingChange(
            @ApiParam(value = "The id of the PendingChange that needs to be deleted",
                      required = true) @PathParam("pendingchange_id") final String pendingChangeId)
            throws AuthorizationException,
            InvalidObjectIdException,
            NotFoundException;

    /**
     * Retrieve the information about the pending change with a known ObjectId
     *
     * @param pendingChangeId the ObjectId of the pending change to get information about
     * @return the definition of the pending change
     * @throws AuthorizationException When the user is not authorized to get information about the pending change
     * @throws InvalidObjectIdException When the provided id is not a valid ObjectId
     * @throws NotFoundException When the pending change could not be found
     */
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{pendingchange_id}")
    @ApiOperation(nickname = "getPendingChange",
                  value = "Get PendingChange data",
                  notes = "Get data of the PendingChange with the provided Id",
                  authorizations = {@Authorization(value = OrchestratorApi.USER_AUTHENTICATION)})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "PendingChange data", response = PendingChangeDescription.class),
            @ApiResponse(code = 400, message = InvalidObjectIdException.INVALID_OBJECT_ID_MESSAGE),
            @ApiResponse(code = 404, message = "PendingChange not found"),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public PendingChangeDescription getPendingChange(
            @ApiParam(value = "The id of the PendingChange that needs to be fetched",
                      required = true) @PathParam("pendingchange_id") final String pendingChangeId)
            throws AuthorizationException,
            InvalidObjectIdException,
            NotFoundException;

    /**
     * List all pending changes that this used is authorized to read.
     *
     * @param page the current page to view (defaults to 1)
     * @param perPage the amount of pending changes to view per page (defaults to {@value
     *            org.flexiblepower.api.OrchestratorApi#DEFAULT_ITEMS_PER_PAGE})
     * @param sortDir the direction to sort the pending changed (defaults to "ASC")
     * @param sortField the field to sort the pending changes on (defaults to "id")
     * @param filters a list of filters in JSON notation (defaults to "{}")
     * @return a list of all pending changes for this user
     * @throws AuthorizationException When the user is not authenticated
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "listPendingChanges",
                  value = "List pending changes",
                  notes = "List all registered PendingChanges",
                  authorizations = {@Authorization(value = OrchestratorApi.ADMIN_AUTHENTICATION)})
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                         message = "An array of PendingChanges",
                         response = PendingChangeDescription.class,
                         responseContainer = "List"),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public Response listPendingChanges(@QueryParam("_page") @DefaultValue("1") int page,
            @QueryParam("_perPage") @DefaultValue(OrchestratorApi.DEFAULT_ITEMS_PER_PAGE) int perPage,
            @QueryParam("_sortDir") @DefaultValue("ASC") String sortDir,
            @QueryParam("_sortField") @DefaultValue("id") String sortField,
            @QueryParam("_filters") @DefaultValue("{}") String filters) throws AuthorizationException;

    /**
     * Clean up all pending changes that are either lingering or are in the FAILED_PERMANENTLY state, or inactive for a
     * long period of time. The exact criteria on what defines a long period of time depends on the implementation.
     *
     * @throws AuthorizationException when the user is not logged in as an administrator
     */
    @DELETE
    @Path("/clean")
    @ApiOperation(nickname = "cleanPendingChanges",
                  value = "Clean PendingChange",
                  notes = "Clean up all lingering and permanently failed PendingChanges",
                  code = 204,
                  authorizations = {@Authorization(value = OrchestratorApi.ADMIN_AUTHENTICATION)})
    @ApiResponses(value = {@ApiResponse(code = 204, message = "PendingChanges cleaned"),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public void cleanPendingChanges() throws AuthorizationException;
}
