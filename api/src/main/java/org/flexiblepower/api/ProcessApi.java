/**
 * File ProcessApi.java
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

import java.util.List;

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

import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.InvalidObjectIdException;
import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.model.Process;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * ProcessApi
 *
 * @version 0.1
 * @since Apr 7, 2017
 */
@Path("process")
@Api("Process")
public interface ProcessApi {

    /**
     * Error message to display if the process is not found
     */
    public static final String PROCESS_NOT_FOUND_MESSAGE = "Process not found";

    /**
     * List all existing processes for the current logged in user. By design if the user is an administrator, all
     * processes are returned.
     *
     * @param page The current page to view (defaults to 1)
     * @param perPage The amount of processes to view per page (defaults to
     *            {@value org.flexiblepower.api.OrchestratorApi#DEFAULT_ITEMS_PER_PAGE})
     * @param sortDir The direction to sort the processes (defaults to "ASC")
     * @param sortField The field to sort the processes on (defaults to "id")
     * @param filters A list of filters in JSON notation
     * @return A list of processes for the logged in user
     * @throws AuthorizationException If the user is not authorized to get information about this process
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "listProcesses",
                  value = "List processes",
                  notes = "List all processes that are currently running",
                  authorizations = {@Authorization(value = OrchestratorApi.USER_AUTHENTICATION)})
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                         message = "An array of Processes",
                         response = Process.class,
                         responseContainer = "List"),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public List<Process> listProcesses(@QueryParam("_page") @DefaultValue("1") final int page,
            @QueryParam("_perPage") @DefaultValue(OrchestratorApi.DEFAULT_ITEMS_PER_PAGE) int perPage,
            @QueryParam("_sortDir") @DefaultValue("ASC") String sortDir,
            @QueryParam("_sortField") @DefaultValue("id") String sortField,
            @QueryParam("_filters") @DefaultValue("{}") String filters) throws AuthorizationException;

    /**
     * Get the process with the specified Id.
     *
     * @param processId The id of the process to look up
     * @return Connection with the specified Id
     * @throws AuthorizationException If the user is not authorized to get information about this process
     * @throws ProcessNotFoundException When no process with the provided id is found
     * @throws InvalidObjectIdException When the provided id is not a valid ObjectId
     */
    @GET
    @Path("{processId}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
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
                      required = true) @PathParam("processId") final String processId)
            throws AuthorizationException,
            ProcessNotFoundException,
            InvalidObjectIdException;

    /**
     * Attempt to update the information of the process with the provided specification.
     *
     * @param processId the ObjectId of the process to update
     * @param process the updated specification of the new process
     * @return the updated process
     * @throws AuthorizationException if the user is not authorized to get information about the specified process
     * @throws ProcessNotFoundException if the process could not be found
     * @throws InvalidObjectIdException if the provided ObjectId is not valid
     */
    @PUT
    @Path("{processId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "updateProcess",
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
                      required = true) @PathParam("processId") final String processId,
            @ApiParam(name = "process",
                      value = "The process definition of the new process to add",
                      required = true) final Process process)
            throws AuthorizationException,
            InvalidObjectIdException,
            ProcessNotFoundException;

    /**
     * Attempt to create a new process with provided specification.
     *
     * @param process the specification of the new process
     * @return the created process
     * @throws AuthorizationException if the user is not authorized to create the process with the provided
     *             specification
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
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

    /**
     * Terminate and remove a process
     *
     * @param processId the id of the process to remove
     * @throws AuthorizationException If the user is not authorized to terminate the process
     * @throws ProcessNotFoundException When the process is not found
     * @throws InvalidObjectIdException When the argument processId is an invalid ObjectId
     */
    @DELETE
    @Path("{processId}")
    @Consumes(MediaType.TEXT_PLAIN)
    @ApiOperation(nickname = "removeProcess",
                  value = "Remove a process",
                  notes = "Remove the process with the specified Id",
                  code = 204,
                  authorizations = {@Authorization(value = OrchestratorApi.USER_AUTHENTICATION)})
    @ApiResponses(value = {@ApiResponse(code = 204, message = "Process deleted"),
            @ApiResponse(code = 400, message = InvalidObjectIdException.INVALID_OBJECT_ID_MESSAGE),
            @ApiResponse(code = 404, message = ProcessApi.PROCESS_NOT_FOUND_MESSAGE),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public void removeProcess(
            @ApiParam(name = "processId",
                      value = "The id of process to remove",
                      required = true) @PathParam("processId") final String processId)
            throws AuthorizationException,
            ProcessNotFoundException,
            InvalidObjectIdException;

    /**
     * Trigger the orchestrator to update the configuration of the provided process. This function is intended for the
     * process to use when it starts, and wants to let the orchestrator know that it is ready to receive a
     * configuration.
     *
     * @param processId the ObjectId of the process to trigger
     * @throws ProcessNotFoundException When the process is not found
     * @throws InvalidObjectIdException When the argument processId is an invalid ObjectId
     * @throws AuthorizationException If the user is not the owner of the process
     */
    @PUT
    @Path("trigger/{processId}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "triggerProcessUpdate",
                  value = "Trigger the process to update",
                  notes = "Will send the specified process its configuration and connections",
                  authorizations = {@Authorization(value = OrchestratorApi.USER_AUTHENTICATION)})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Process with the specified Id", response = Process.class),
            @ApiResponse(code = 400, message = InvalidObjectIdException.INVALID_OBJECT_ID_MESSAGE),
            @ApiResponse(code = 404, message = ProcessApi.PROCESS_NOT_FOUND_MESSAGE),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public void triggerProcessConfig(
            @ApiParam(name = "processId",
                      value = "The id of process to trigger",
                      required = true) @PathParam("processId") String processId)
            throws ProcessNotFoundException,
            InvalidObjectIdException,
            AuthorizationException;

}