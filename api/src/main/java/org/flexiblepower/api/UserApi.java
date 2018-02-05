/**
 * File UserApi.java
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
import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.model.User;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * UserApi
 *
 * @version 0.1
 * @since Apr 7, 2017
 */
@Api("User")
@Path("user")
@Produces(MediaType.APPLICATION_JSON)
public interface UserApi {

    /**
     * Error message to display if the user is not found
     */
    static final String USER_NOT_FOUND_MESSAGE = "User not found";

    /**
     * Attempt to create a new user with provided specification.
     *
     * @param newUser the specification of the new process
     * @return the created user
     * @throws AuthorizationException if the user is not an admin
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "createUser",
                  value = "Create user",
                  notes = "Create a new user",
                  response = String.class,
                  authorizations = {@Authorization(value = OrchestratorApi.ADMIN_AUTHENTICATION)})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "New user created", response = String.class),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public User createUser(@ApiParam(value = "The new user to add", required = true) final User newUser)
            throws AuthorizationException;

    /**
     * Attempt to update the information of the user with the provided specification.
     *
     * @param userId the ObjectId of the user to update
     * @param updatedUser the updated specification of the new user
     * @return the updated user
     * @throws AuthorizationException if the user is not an admin
     * @throws NotFoundException if the user could not be found
     * @throws InvalidObjectIdException if the provided ObjectId is not valid
     */
    @PUT
    @Path("/{user_id}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
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
            throws AuthorizationException,
            NotFoundException,
            InvalidObjectIdException;

    /**
     * Remove a user. This is only allowed for an administrator.
     *
     * @param userId the id of the user to remove
     * @throws AuthorizationException If the user is not and admin
     * @throws ProcessNotFoundException When the user is not found
     * @throws InvalidObjectIdException When the argument userId is an invalid ObjectId
     * @throws NotFoundException
     */
    @DELETE
    @Path("/{user_id}")
    @Consumes(MediaType.TEXT_PLAIN)
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
            throws AuthorizationException,
            InvalidObjectIdException,
            NotFoundException;

    /**
     * Get the user with the specified user ID. This function will only return user information if the requested user
     * is equal to the logged in user, or the logged in user is an administrator.
     *
     * @param userId The id of the user to look up
     * @return User with the specified Id
     * @throws AuthorizationException If the user is not authorized to get information about the user.
     * @throws InvalidObjectIdException If the provided userId is not a valid ObjectId
     * @throws NotFoundException When no user with the provided name
     */
    @GET
    @Path("/{user_id}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
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
                    throws AuthorizationException,
                    InvalidObjectIdException,
                    NotFoundException;

    /**
     * Get the user with the specified user name. This function will only return user information if the requested user
     * is equal to the logged in user, or the logged in user is an administrator.
     *
     * @param username The name of the user to look up
     * @return User with the specified name
     * @throws AuthorizationException If the user is not authorized to get information about the user.
     * @throws NotFoundException When no user with the provided name
     */
    @GET
    @Path("/by_username/{username}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "getUser",
                  value = "Get user data",
                  notes = "Get data of the user with the provided user name",
                  authorizations = {@Authorization(value = OrchestratorApi.USER_AUTHENTICATION)})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "User data", response = User.class),
            @ApiResponse(code = 404, message = UserApi.USER_NOT_FOUND_MESSAGE),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public User getUserByUsername(
            @ApiParam(value = "The username of the User that needs to be fetched",
                      required = true) @PathParam("username") final String username)
            throws AuthorizationException,
            NotFoundException;

    /**
     * List all existing users. All users are allowed to list users, but only administrators can receive more than one
     * user. Non-admin users will receive a list containing only the user information about themselves.
     *
     * @param page the current page to view (defaults to 1)
     * @param perPage the amount of users to view per page (defaults to
     *            {@value OrchestratorApi#DEFAULT_ITEMS_PER_PAGE})
     * @param sortDir the direction to sort the users (defaults to "ASC")
     * @param sortField the field to sort the users on (defaults to "id")
     * @param filters a list of filters in JSON notation
     * @return A list of users in the dEF-Pi environment.
     * @throws AuthorizationException When the user is not authenticated
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
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
