package org.flexiblepower.api;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
    @ApiResponses(value = {@ApiResponse(code = 200,
                                        message = "The list of interfaces",
                                        response = Interface.class,
                                        responseContainer = "List")})
    public List<Interface> listInterfaces();

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "newInterface",
                  value = "Add new interface",
                  notes = "Add a new interface to the registry",
                  authorizations = {@Authorization(value = OrchestratorApi.ADMIN_AUTHENTICATION)})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The sha256 hash of the new interface", response = String.class),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public String newInterface(
            @ApiParam(name = "interface", value = "The interface to add", required = true) final Interface itf)
            throws AuthorizationException;

    @GET
    @Path("{sha256}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiOperation(nickname = "downloadInterface",
                  value = "Download interface",
                  notes = "Downloads the interface with the provided hash",
                  authorizations = {@Authorization(value = OrchestratorApi.USER_AUTHENTICATION)})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The interface", response = Interface.class),
            @ApiResponse(code = 404, message = InterfaceApi.INTERFACE_NOT_FOUND_MESSAGE),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public Response downloadInterface(
            @ApiParam(name = "sha256",
                      value = "The sha256 hash of the interface to download",
                      required = true) @PathParam("sha256") final String sha256)
            throws AuthorizationException,
            NotFoundException;

    @DELETE
    @Path("{sha256}")
    @ApiOperation(nickname = "deleteInterface",
                  value = "Delete interface",
                  notes = "Delete the interface with the provided hash",
                  code = 204,
                  authorizations = {@Authorization(value = OrchestratorApi.ADMIN_AUTHENTICATION)})
    @ApiResponses(value = {@ApiResponse(code = 204, message = "The interface was deleted"),
            @ApiResponse(code = 404, message = InterfaceApi.INTERFACE_NOT_FOUND_MESSAGE),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public void deleteInterface(
            @ApiParam(name = "sha256",
                      value = "The sha256 hash of the interface to delete",
                      required = true) @PathParam("sha256") final String sha256)
            throws AuthorizationException,
            NotFoundException;
}
