package io.swagger.api;

import io.swagger.model.*;
import io.swagger.api.UserApiService;
import io.swagger.api.factories.UserApiServiceFactory;

import io.swagger.annotations.ApiParam;
import io.swagger.jaxrs.*;

import io.swagger.model.Error;
import io.swagger.model.User;

import java.util.List;
import io.swagger.api.NotFoundException;

import java.io.InputStream;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.*;
import javax.validation.constraints.*;

@Path("/user")

@Produces({ "application/json" })
@io.swagger.annotations.Api(description = "the user API")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2017-03-20T14:11:17.160Z")
public class UserApi  {
   private final UserApiService delegate = UserApiServiceFactory.getUserApi();

    @POST
    
    
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Create a new user", notes = "", response = User.class, authorizations = {
        @io.swagger.annotations.Authorization(value = "AdminSecurity")
    }, tags={ "User", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 201, message = "New user created", response = User.class),
        
        @io.swagger.annotations.ApiResponse(code = 200, message = "Unexpected error", response = User.class) })
    public Response createUser(@Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.createUser(securityContext);
    }
    @DELETE
    @Path("/{user_id}")
    
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Delete this user", notes = "", response = User.class, authorizations = {
        @io.swagger.annotations.Authorization(value = "AdminSecurity")
    }, tags={ "User", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "User data", response = User.class),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "User not found", response = User.class) })
    public Response deleteUser(@ApiParam(value = "The id that needs to be fetched",required=true) @PathParam("user_id") String userId
,@Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.deleteUser(userId,securityContext);
    }
    @GET
    @Path("/{user_id}")
    
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Get user data", notes = "", response = User.class, authorizations = {
        @io.swagger.annotations.Authorization(value = "UserSecurity")
    }, tags={ "User", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "User data", response = User.class),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "User not found", response = User.class) })
    public Response getUserById(@ApiParam(value = "The id of the User that needs to be fetched",required=true) @PathParam("user_id") String userId
,@Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.getUserById(userId,securityContext);
    }
    @GET
    
    
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "List users", notes = "", response = User.class, responseContainer = "List", authorizations = {
        @io.swagger.annotations.Authorization(value = "AdminSecurity")
    }, tags={ "User", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "An array of Users", response = User.class, responseContainer = "List") })
    public Response listUsers(@Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.listUsers(securityContext);
    }
}
