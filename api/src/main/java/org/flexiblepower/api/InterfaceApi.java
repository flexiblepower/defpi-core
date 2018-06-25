/**
 * File InterfaceApi.java
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
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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

/**
 * This is the interface of the orchestrator Interface API.
 *
 * @version 0.1
 * @since Apr 7, 2017
 */
@Api("Interface")
@Path("interface")
public interface InterfaceApi {

    /**
     * Error message to display if the interface is not found
     */
    public static final String INTERFACE_NOT_FOUND_MESSAGE = "Interface not found";

    /**
     * List all interfaces that are available
     *
     * @param page the current page to view (defaults to 1)
     * @param perPage the amount of interfaces to view per page (defaults to
     *            {@value org.flexiblepower.api.OrchestratorApi#DEFAULT_ITEMS_PER_PAGE})
     * @param sortDir the direction to sort the interfaces (defaults to "ASC")
     * @param sortField the field to sort the interfaces on (defaults to "id")
     * @param filters a list of filters in JSON notation
     * @return A list of all interfaces
     * @throws AuthorizationException if the user is not authenticated at all
     */
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
    public List<Interface> listInterfaces(@QueryParam("_page") @DefaultValue("1") int page,
            @QueryParam("_perPage") @DefaultValue(OrchestratorApi.DEFAULT_ITEMS_PER_PAGE) int perPage,
            @QueryParam("_sortDir") @DefaultValue("ASC") String sortDir,
            @QueryParam("_sortField") @DefaultValue("id") String sortField,
            @QueryParam("_filters") @DefaultValue("{}") String filters) throws AuthorizationException;

    /**
     * Gets the interface with the provided id
     *
     * @param id The id of the interface
     * @return The interface with the provided id
     * @throws NotFoundException If the referenced interface could not be found
     * @throws AuthorizationException if the user is not authenticated at all
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
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
