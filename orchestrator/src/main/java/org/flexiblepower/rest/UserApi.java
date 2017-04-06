package org.flexiblepower.rest;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.bson.types.ObjectId;
import org.flexiblepower.exceptions.ApiException;
import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.model.User;

import lombok.extern.slf4j.Slf4j;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Slf4j
@Path("/user")
@Api("User")
@Produces(MediaType.APPLICATION_JSON)
public class UserApi extends BaseApi {

    private static final String USER_NOT_FOUND_MESSAGE = "User not found";

    protected UserApi(@Context final HttpHeaders httpHeaders) {
        super(httpHeaders);
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Create user",
                  notes = "Create a new user",
                  response = User.class,
                  authorizations = {@Authorization(value = "AdminSecurity")})
    @ApiResponses(value = {@ApiResponse(code = 201, message = "New user created", response = User.class),
            @ApiResponse(code = 200, message = "Unexpected error", response = User.class)})
    public String createUser(final User newUser) throws AuthorizationException {
        // Update the password to store it encrypted
        newUser.setPassword(newUser.getPassword());
        return this.db.insertUser(newUser);
    }

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
                      required = true) @PathParam("user_id") final String userId,
            @Context final SecurityContext securityContext) throws AuthorizationException {
        this.db.deleteUser(userId);
        return userId;
    }

    @GET
    @Path("/{user_id}")
    @ApiOperation(value = "Get user data",
                  notes = "Get data of the user with the provided Id",
                  response = User.class,
                  authorizations = {@Authorization(value = "UserSecurity")})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "User data", response = User.class),
            @ApiResponse(code = 404, message = UserApi.USER_NOT_FOUND_MESSAGE, response = void.class)})
    public User
            getUserById(
                    @ApiParam(value = "The id of the User that needs to be fetched",
                              required = true) @PathParam("user_id") final String userId)
                    throws AuthorizationException {
        if (!ObjectId.isValid(userId)) {
            throw new ApiException(Status.BAD_REQUEST, "UserId '" + userId + "' is not a valid User Id");
        }

        final ObjectId oid = new ObjectId(userId);
        final User ret = this.db.getUser(oid);
        if (ret == null) {
            throw new ApiException(Status.NOT_FOUND, UserApi.USER_NOT_FOUND_MESSAGE);
        } else {
            return ret;
        }
    }

    @GET
    @ApiOperation(value = "List users",
                  notes = "List all registered users",
                  response = User.class,
                  responseContainer = "List",
                  authorizations = {@Authorization(value = "AdminSecurity")})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "An array of Users", response = User.class, responseContainer = "List")})
    public List<User> listUsers() throws AuthorizationException {
        return this.db.getUsers();
    }
}
