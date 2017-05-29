package org.flexiblepower.api;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.InvalidObjectIdException;
import org.flexiblepower.exceptions.NotFoundException;
import org.flexiblepower.model.Process;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Path("process")
@Api("Process")
@Produces(MediaType.APPLICATION_JSON)
public interface ProcessApi {

    public static final String PROCESS_NOT_FOUND_MESSAGE = "Process not found";

    @GET
    @ApiOperation(nickname = "listProcesses",
                  value = "List processes",
                  notes = "List all processes that are currently running",
                  authorizations = {@Authorization(value = OrchestratorApi.USER_AUTHENTICATION)})
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                         message = "An array of Processes",
                         response = Process.class,
                         responseContainer = "List")})
    public List<org.flexiblepower.model.Process> listProcesses();

    @GET
    @Path("{processId}")
    @ApiOperation(nickname = "getProcess",
                  value = "Get process data",
                  notes = "Get the data of the process with the specified Id",
                  authorizations = {@Authorization(value = OrchestratorApi.USER_AUTHENTICATION)})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Process with the specified Id", response = Process.class),
            @ApiResponse(code = 400, message = InvalidObjectIdException.INVALID_OBJECT_ID_MESSAGE),
            @ApiResponse(code = 404, message = ProcessApi.PROCESS_NOT_FOUND_MESSAGE),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public Process getProcess(
            @ApiParam(name = "processId",
                      value = "The id of the process",
                      required = true) @PathParam("processId") final String uuid)
            throws AuthorizationException, NotFoundException, InvalidObjectIdException;

    @PUT
    @Path("{processId}")
    @ApiOperation(nickname = "getProcess",
                  value = "Get process data",
                  notes = "Get the data of the process with the specified Id",
                  authorizations = {@Authorization(value = OrchestratorApi.USER_AUTHENTICATION)})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Process with the specified Id", response = Process.class),
            @ApiResponse(code = 400, message = InvalidObjectIdException.INVALID_OBJECT_ID_MESSAGE),
            @ApiResponse(code = 404, message = ProcessApi.PROCESS_NOT_FOUND_MESSAGE),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public Process updateProcess(
            @ApiParam(name = "processId",
                      value = "The id of the process",
                      required = true) @PathParam("processId") final String uuid,
            @ApiParam(name = "process",
                      value = "The process definition of the new process to add",
                      required = true) final Process process)
            throws AuthorizationException;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "newProcess",
                  value = "Create a process",
                  notes = "Create a new process",
                  authorizations = {@Authorization(value = OrchestratorApi.USER_AUTHENTICATION)})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Id of the new process", response = Process.class),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public Process
            newProcess(@ApiParam(name = "process",
                                 value = "The process definition of the new process to add",
                                 required = true) final Process process)
                    throws AuthorizationException;

    @DELETE
    @Path("{processId}")
    @ApiOperation(nickname = "removeProcess",
                  value = "Remove a process",
                  notes = "Remove the process with the specified Id",
                  code = 204,
                  authorizations = {@Authorization(value = OrchestratorApi.USER_AUTHENTICATION)})
    @ApiResponses(value = {@ApiResponse(code = 204, message = "Process deleted"),
            @ApiResponse(code = 404, message = ProcessApi.PROCESS_NOT_FOUND_MESSAGE),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public void removeProcess(
            @ApiParam(name = "process",
                      value = "The id of process to remove",
                      required = true) @PathParam("processId") final String uuid)
            throws AuthorizationException, NotFoundException;

}