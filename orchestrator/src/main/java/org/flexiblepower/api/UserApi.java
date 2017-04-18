package org.flexiblepower.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.InvalidObjectIdException;
import org.flexiblepower.exceptions.NotFoundException;
import org.flexiblepower.model.User;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Api("User")
@Path("user")
@Produces(MediaType.APPLICATION_JSON)
public interface UserApi {

    static final String USER_NOT_FOUND_MESSAGE = "User not found";

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "createUser",
                  value = "Create user",
                  notes = "Create a new user",
                  response = String.class,
                  authorizations = {@Authorization(value = OrchestratorApi.ADMIN_AUTHENTICATION)})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "New user created", response = String.class),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public User createUser(@ApiParam(value = "The new user to add", required = true) final User newUser)
            throws AuthorizationException;

    @PUT
    @Path("/{user_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "updateUser",
                  value = "Update user",
                  notes = "Update a user",
                  response = String.class,
                  authorizations = {@Authorization(value = OrchestratorApi.ADMIN_AUTHENTICATION)})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "User updated", response = String.class),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public User updateUser(
            @ApiParam(value = "The id of the user that needs to be deleted",
                      required = true) @PathParam("user_id") final String userId,
            @ApiParam(value = "The user to update", required = true) final User updatedUser)
            throws AuthorizationException;

    @DELETE
    @Path("/{user_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "deleteUser",
                  value = "Delete user",
                  notes = "Delete the user with the provided Id",
                  code = 204,
                  authorizations = {@Authorization(value = OrchestratorApi.ADMIN_AUTHENTICATION)})
    @ApiResponses(value = {@ApiResponse(code = 204, message = "User deleted"),
            @ApiResponse(code = 400, message = InvalidObjectIdException.INVALID_OBJECT_ID_MESSAGE),
            @ApiResponse(code = 404, message = UserApi.USER_NOT_FOUND_MESSAGE),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public void deleteUser(
            @ApiParam(value = "The id of the user that needs to be deleted",
                      required = true) @PathParam("user_id") final String userId)
            throws AuthorizationException, InvalidObjectIdException, NotFoundException;

    @GET
    @Path("/{user_id}")
    @ApiOperation(nickname = "getUser",
                  value = "Get user data",
                  notes = "Get data of the user with the provided Id",
                  authorizations = {@Authorization(value = OrchestratorApi.USER_AUTHENTICATION)})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "User data", response = User.class),
            @ApiResponse(code = 400, message = InvalidObjectIdException.INVALID_OBJECT_ID_MESSAGE),
            @ApiResponse(code = 404, message = UserApi.USER_NOT_FOUND_MESSAGE),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public User
            getUser(@ApiParam(value = "The id of the User that needs to be fetched",
                              required = true) @PathParam("user_id") final String userId)
                    throws AuthorizationException, InvalidObjectIdException, NotFoundException;

    @GET
    @ApiOperation(nickname = "listUsers",
                  value = "List users",
                  notes = "List all registered users",
                  authorizations = {@Authorization(value = OrchestratorApi.ADMIN_AUTHENTICATION)})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "An array of Users", response = User.class, responseContainer = "List"),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public Response listUsers(@QueryParam("_page") @DefaultValue("1") int page,
            @QueryParam("_perPage") @DefaultValue("1000") int perPage,
            @QueryParam("_sortDir") @DefaultValue("ASC") String sortDir,
            @QueryParam("_sortField") @DefaultValue("id") String sortField,
            @QueryParam("_filters") @DefaultValue("{}") String filters) throws AuthorizationException;
}
