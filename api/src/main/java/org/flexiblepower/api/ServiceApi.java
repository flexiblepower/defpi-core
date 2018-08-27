/*-
 * #%L
 * dEF-Pi API
 * %%
 * Copyright (C) 2017 - 2018 Flexible Power Alliance Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
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
import org.flexiblepower.exceptions.RepositoryNotFoundException;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.Service;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * ServiceApi
 *
 * @version 0.1
 * @since Apr 7, 2017
 */
@Api("Service")
@Path("/service")
public interface ServiceApi {

    /**
     * List all existing services. All users are allowed to list services, so only a check is performed to see if the
     * user is authenticated.
     *
     * @param page the current page to view (defaults to 1)
     * @param perPage the amount of services to view per page (defaults to
     *            {@value org.flexiblepower.api.OrchestratorApi#DEFAULT_ITEMS_PER_PAGE})
     * @param sortDir the direction to sort the services (defaults to "ASC")
     * @param sortField the field to sort the services on (defaults to "id")
     * @param filters a list of filters in JSON notation
     * @return A list of services in the service registry
     * @throws AuthorizationException When the user is not authenticated
     */
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
    public List<Service> listServices(@QueryParam("_page") @DefaultValue("1") int page,
            @QueryParam("_perPage") @DefaultValue(OrchestratorApi.DEFAULT_ITEMS_PER_PAGE) int perPage,
            @QueryParam("_sortDir") @DefaultValue("ASC") String sortDir,
            @QueryParam("_sortField") @DefaultValue("id") String sortField,
            @QueryParam("_filters") @DefaultValue("{}") String filters) throws AuthorizationException;

    /**
     * Get the service with the specified Id.
     *
     * @param id The id of the service to retrieve from the registry
     * @return The service with the specified Id
     * @throws ServiceNotFoundException if no service is found with the specified id
     * @throws AuthorizationException if the user is not authenticated
     */
    @GET
    @Path("/{id}")
    @Consumes(MediaType.TEXT_PLAIN)
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
            throws ServiceNotFoundException,
            AuthorizationException;

    /**
     * Get all versions of a specific service
     *
     * @param id The id of the service to retrieve from the registry
     * @return A list with all versions of the service with the specified Id
     * @throws ServiceNotFoundException if no service is found with the specified id
     * @throws AuthorizationException if the user is not authenticated
     */
    @GET
    @Path("/{id}/all")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "getAllService",
                  value = "Get all versions of a service",
                  notes = "Get all versions of a particular service from the registry",
                  authorizations = {@Authorization(value = OrchestratorApi.USER_AUTHENTICATION)})
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                         message = "The definition of the service",
                         response = Service.class,
                         responseContainer = "List"),
            @ApiResponse(code = 404, message = ServiceNotFoundException.SERVICE_NOT_FOUND_MESSAGE),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public List<Service> getAllServiceVersions(
            @ApiParam(name = "id", value = "The id of the service", required = true) @PathParam("id") final String id)
            throws ServiceNotFoundException,
            AuthorizationException;

}
