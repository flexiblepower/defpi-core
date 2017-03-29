package org.flexiblepower.rest;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.flexiblepower.exceptions.ApiException;
import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.model.User;
import org.flexiblepower.orchestrator.MongoDbConnector;

import lombok.extern.slf4j.Slf4j;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Slf4j
@Path("/user")
@Produces(MediaType.APPLICATION_JSON)
@Api(description = "the user API")
public class UserApi {

    private final MongoDbConnector db = new MongoDbConnector();

    @POST
    @Produces("application/json")
    @ApiOperation(value = "Create a new user",
                  notes = "",
                  response = User.class,
                  authorizations = {@Authorization(value = "AdminSecurity")},
                  tags = {"User",})
    @ApiResponses(value = {@ApiResponse(code = 201, message = "New user created", response = User.class),
            @ApiResponse(code = 200, message = "Unexpected error", response = User.class)})
    public String createUser(@Context final SecurityContext securityContext, final User user) {
        try {
            return this.db.insertUser(user);
        } catch (final AuthorizationException e) {
            UserApi.log.warn("Unauthorized call to create user");
            throw new ApiException(Status.UNAUTHORIZED, e);
        }
    }

    @DELETE
    @Path("/{user_id}")
    @Produces({"application/json"})
    @ApiOperation(value = "Delete this user",
                  notes = "",
                  response = User.class,
                  authorizations = {@Authorization(value = "AdminSecurity")},
                  tags = {"User",})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "User data", response = User.class),
            @ApiResponse(code = 404, message = "User not found", response = User.class)})
    public String deleteUser(
            @ApiParam(value = "The id that needs to be fetched",
                      required = true) @PathParam("user_id") final String userId,
            @Context final SecurityContext securityContext) {
        try {
            this.db.deleteUser(userId);
            return userId;
        } catch (final AuthorizationException e) {
            UserApi.log.warn("Unauthorized call to delete user {}", userId);
            throw new ApiException(Status.UNAUTHORIZED, e);
        }
    }

    @GET
    @Path("/{user_id}")
    @Produces({"application/json"})
    @ApiOperation(value = "Get user data",
                  notes = "",
                  response = User.class,
                  authorizations = {@Authorization(value = "UserSecurity")},
                  tags = {"User",})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "User data", response = User.class),
            @ApiResponse(code = 404, message = "User not found", response = User.class)})
    public User getUserById(
            @ApiParam(value = "The id of the User that needs to be fetched",
                      required = true) @PathParam("user_id") final String userId,
            @Context final SecurityContext securityContext) {
        try {
            return this.db.getUser(userId);
        } catch (final AuthorizationException e) {
            UserApi.log.warn("Unauthorized call to obtain user {}", userId);
            throw new ApiException(Status.UNAUTHORIZED, e);
        }
    }

    @GET
    @Produces({"application/json"})
    @ApiOperation(value = "List users",
                  notes = "",
                  response = User.class,
                  responseContainer = "List",
                  authorizations = {@Authorization(value = "AdminSecurity")},
                  tags = {"User",})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "An array of Users", response = User.class, responseContainer = "List")})
    public List<User> listUsers(@Context final SecurityContext securityContext) {
        try {
            return this.db.getUsers();
        } catch (final AuthorizationException e) {
            UserApi.log.warn("Unauthorized call to list all users");
            throw new ApiException(Status.UNAUTHORIZED, e);
        }
    }
}
