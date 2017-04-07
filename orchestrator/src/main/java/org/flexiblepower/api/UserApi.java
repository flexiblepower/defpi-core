package org.flexiblepower.api;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.InvalidObjectIdException;
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
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Create user",
                  notes = "Create a new user",
                  response = User.class,
                  authorizations = {@Authorization(value = "AdminSecurity")})
    @ApiResponses(value = {@ApiResponse(code = 201, message = "New user created", response = User.class),
            @ApiResponse(code = 200, message = "Unexpected error", response = User.class)})
    public String createUser(final User newUser) throws AuthorizationException;

    @DELETE
    @Path("/{user_id}")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Delete user",
                  notes = "Delete the user with the provided Id",
                  response = User.class,
                  authorizations = {@Authorization(value = "AdminSecurity")})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "User data", response = User.class),
            @ApiResponse(code = 404, message = UserApi.USER_NOT_FOUND_MESSAGE, response = User.class)})
    public String deleteUser(
            @ApiParam(value = "The id of the user that needs to be deleted",
                      required = true) @PathParam("user_id") final String userId)
            throws AuthorizationException,
            InvalidObjectIdException;

    @GET
    @Path("/{user_id}")
    @ApiOperation(value = "Get user data",
                  notes = "Get data of the user with the provided Id",
                  response = User.class,
                  authorizations = {@Authorization(value = "UserSecurity")})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "User data", response = User.class),
            @ApiResponse(code = 404, message = UserApi.USER_NOT_FOUND_MESSAGE, response = void.class)})
    public User getUserById(
            @ApiParam(value = "The id of the User that needs to be fetched",
                      required = true) @PathParam("user_id") final String userId)
            throws AuthorizationException,
            InvalidObjectIdException;

    @GET
    @ApiOperation(value = "List users",
                  notes = "List all registered users",
                  response = User.class,
                  responseContainer = "List",
                  authorizations = {@Authorization(value = "AdminSecurity")})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "An array of Users", response = User.class, responseContainer = "List")})
    public List<User> listUsers() throws AuthorizationException;
}
