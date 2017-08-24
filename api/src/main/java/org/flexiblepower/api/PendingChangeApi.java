package org.flexiblepower.api;

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

@Api("PendingChange")
@Path("pendingchange")
@Produces(MediaType.APPLICATION_JSON)
public interface PendingChangeApi {

    @DELETE
    @Path("/{pendingchange_id}")
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
                      required = true) @PathParam("pendingchange_id") final String PendingChangeId)
            throws AuthorizationException, InvalidObjectIdException, NotFoundException;

    @GET
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
                      required = true) @PathParam("pendingchange_id") final String pendingChange)
            throws AuthorizationException, InvalidObjectIdException, NotFoundException;

    @GET
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
            @QueryParam("_perPage") @DefaultValue("1000") int perPage,
            @QueryParam("_sortDir") @DefaultValue("ASC") String sortDir,
            @QueryParam("_sortField") @DefaultValue("id") String sortField,
            @QueryParam("_filters") @DefaultValue("{}") String filters) throws AuthorizationException;
}
