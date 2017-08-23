package org.flexiblepower.api;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.NotFoundException;
import org.flexiblepower.exceptions.RepositoryNotFoundException;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.Service;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Api("Service")
@Path("/service")
@Produces(MediaType.APPLICATION_JSON)
public interface ServiceApi {

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "listServices",
                  value = "List Services",
                  notes = "List all services in a repository",
                  authorizations = {@Authorization(value = OrchestratorApi.USER_AUTHENTICATION)})
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                         message = "An array of services",
                         response = Service.class,
                         responseContainer = "List"),
            @ApiResponse(code = 404, message = RepositoryNotFoundException.REPOSITORY_NOT_FOUND_MESSAGE),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public List<Service> listServices() throws NotFoundException, AuthorizationException;

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "getService",
                  value = "Get a service",
                  notes = "Get a particular service from the registry",
                  authorizations = {@Authorization(value = OrchestratorApi.USER_AUTHENTICATION)})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The definition of the service", response = Service.class),
            @ApiResponse(code = 404, message = ServiceNotFoundException.SERVICE_NOT_FOUND_MESSAGE),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public Service getService(
            @ApiParam(name = "id", value = "The id of the service", required = true) @PathParam("id") final String id)
            throws NotFoundException,
            AuthorizationException;

}
