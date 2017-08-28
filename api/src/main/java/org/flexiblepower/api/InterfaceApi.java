package org.flexiblepower.api;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.NotFoundException;
import org.flexiblepower.model.Interface;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Api("Interface")
@Path("interface")
public interface InterfaceApi {

    public static final String INTERFACE_NOT_FOUND_MESSAGE = "Interface not found";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "listInterfaces",
                  value = "List all interfaces",
                  notes = "List all interfaces that are available",
                  authorizations = {@Authorization(value = OrchestratorApi.USER_AUTHENTICATION)})
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                         message = "The list of interfaces",
                         response = Interface.class,
                         responseContainer = "List"),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public List<Interface> listInterfaces() throws AuthorizationException;

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "getInterface",
                  value = "Get interface",
                  notes = "Gets the interface with the provided id",
                  authorizations = {@Authorization(value = OrchestratorApi.USER_AUTHENTICATION)})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The interface", response = Interface.class),
            @ApiResponse(code = 404, message = InterfaceApi.INTERFACE_NOT_FOUND_MESSAGE)})
    public Interface getInterface(
            @ApiParam(name = "id", value = "The id of the interface", required = true) @PathParam("id") final String id)
            throws NotFoundException,
            AuthorizationException;

}
