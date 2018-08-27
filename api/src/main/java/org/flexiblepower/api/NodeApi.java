/**
 * File NodeApi.java
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

import org.flexiblepower.exceptions.*;
import org.flexiblepower.model.NodePool;
import org.flexiblepower.model.PrivateNode;
import org.flexiblepower.model.PublicNode;
import org.flexiblepower.model.UnidentifiedNode;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * NodeApi
 *
 * @version 0.1
 * @since Apr 7, 2017
 */
@Api("Node")
@Path("/")
public interface NodeApi {

    /**
     * Error message to display if the private node is not found
     */
    final static String PRIVATE_NODE_NOT_FOUND_MESSAGE = "Private node not found";
    /**
     * Error message to display if the public node is not found
     */
    final static String PUBLIC_NODE_NOT_FOUND_MESSAGE = "Public node not found";
    /**
     * Error message to display if the unidentified node is not found
     */
    final static String UNIDENTIFIED_NODE_NOT_FOUND_MESSAGE = "Unidentified node not found";

    /**
     * Create a new private node from an unidentified node.
     *
     * @param newNode the definition of the new private node to create
     * @return the newly created private node
     * @throws AuthorizationException If the user is not authorized to create the private node
     * @throws NotFoundException When the unidentified node that is referred to is not found
     * @throws InvalidObjectIdException When the argument newNode contains an invalid ObjectId
     */
    @POST
    @Path("/privatenode")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "createPrivateNode",
                  value = "Create a new private node",
                  notes = "Create a private node based on the id of an unidentified node",
                  authorizations = {@Authorization(value = OrchestratorApi.ADMIN_AUTHENTICATION)})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Private node succesfully created", response = PrivateNode.class),
            @ApiResponse(code = 400, message = InvalidObjectIdException.INVALID_OBJECT_ID_MESSAGE),
            @ApiResponse(code = 404, message = NodeApi.UNIDENTIFIED_NODE_NOT_FOUND_MESSAGE),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public PrivateNode createPrivateNode(
            @ApiParam(name = "newNode",
                      value = "The definition of the node to create",
                      required = true) final PrivateNode newNode)
            throws AuthorizationException,
            NotFoundException,
            InvalidObjectIdException;

    /**
     * Create a new public node from an unidentified node
     *
     * @param newNode the definition of the new public node to create
     * @return the newly created private node
     * @throws AuthorizationException If the user is not authorized to create the public node
     * @throws NotFoundException When the unidentified node, or the nodepool that is referred to is not found
     * @throws InvalidObjectIdException When the argument newNode contains an invalid ObjectId
     */
    @POST
    @Path("/publicnode")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "createPublicNode",
                  value = "Create a new public node",
                  notes = "Create a public node based on the id of an unidentified node",
                  authorizations = {@Authorization(value = OrchestratorApi.ADMIN_AUTHENTICATION)})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Public node succesfully created", response = PublicNode.class),
            @ApiResponse(code = 400, message = InvalidObjectIdException.INVALID_OBJECT_ID_MESSAGE),
            @ApiResponse(code = 404, message = NodeApi.UNIDENTIFIED_NODE_NOT_FOUND_MESSAGE),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public PublicNode createPublicNode(
            @ApiParam(name = "newNode",
                      value = "The definition of the node to create",
                      required = true) final PublicNode newNode)
            throws AuthorizationException,
            NotFoundException,
            InvalidObjectIdException;

    /**
     * Update the information of the private node with the provided specification.
     *
     * @param nodeId the ObjectId of the node to update
     * @param updatedPrivateNode the updated information of the node
     * @return the updated node
     * @throws AuthorizationException if the user is not logged in as an administrator.
     * @throws NotFoundException if the node could not be found
     * @throws InvalidObjectIdException if the provided ObjectId is not valid
     */
    @PUT
    @Path("/privatenode/{node_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "updatePrivateNode",
                  value = "Update Private Node",
                  notes = "Update a Private Node",
                  authorizations = {@Authorization(value = OrchestratorApi.ADMIN_AUTHENTICATION)})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Node updated", response = PrivateNode.class),
            @ApiResponse(code = 400, message = InvalidObjectIdException.INVALID_OBJECT_ID_MESSAGE),
            @ApiResponse(code = 404, message = NodeApi.PRIVATE_NODE_NOT_FOUND_MESSAGE),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public PrivateNode updatePrivateNode(
            @ApiParam(value = "The id of the node that should be updated",
                      required = true) @PathParam("node_id") final String nodeId,
            @ApiParam(value = "The PrivateNode to update", required = true) final PrivateNode updatedPrivateNode)
            throws AuthorizationException,
            NotFoundException,
            InvalidObjectIdException;

    /**
     * Update the information of the public node with the provided specification.
     *
     * @param nodeId the ObjectId of the node to update
     * @param updatedPublicNode the updated information of the node
     * @return the updated node
     * @throws AuthorizationException if the user is not logged in as an administrator.
     * @throws NotFoundException if the node could not be found
     * @throws InvalidObjectIdException if the provided ObjectId is not valid
     */
    @PUT
    @Path("/publicnode/{node_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "updatePublicNode",
                  value = "Update Publc Node",
                  notes = "Update a Publc Node",
                  authorizations = {@Authorization(value = OrchestratorApi.ADMIN_AUTHENTICATION)})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Node updated", response = PublicNode.class),
            @ApiResponse(code = 400, message = InvalidObjectIdException.INVALID_OBJECT_ID_MESSAGE),
            @ApiResponse(code = 404, message = NodeApi.PUBLIC_NODE_NOT_FOUND_MESSAGE),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public PublicNode updatePublicNode(
            @ApiParam(value = "The id of the node that should be updated",
                      required = true) @PathParam("node_id") final String nodeId,
            @ApiParam(value = "The PublicNode to update", required = true) final PublicNode updatedPublicNode)
            throws AuthorizationException,
            NotFoundException,
            InvalidObjectIdException;

    /**
     * Remove a private node, and return it to being an unidentified node
     *
     * @param nodeId the id of the node to remove
     * @throws AuthorizationException If the user is not authorized to delete the private node
     * @throws NotFoundException When the private node that is referred to is not found
     * @throws InvalidObjectIdException When the argument nodeId is an invalid ObjectId
     */
    @DELETE
    @Path("/privatenode/{node_id}")
    @Consumes(MediaType.TEXT_PLAIN)
    @ApiOperation(nickname = "deletePrivateNode",
                  value = "Remove a private node",
                  notes = "Remove a private node and make it unidentified again",
                  code = 204,
                  authorizations = {@Authorization(value = OrchestratorApi.ADMIN_AUTHENTICATION)})
    @ApiResponses(value = {@ApiResponse(code = 204, message = "Private node succesfully deleted"),
            @ApiResponse(code = 400, message = InvalidObjectIdException.INVALID_OBJECT_ID_MESSAGE),
            @ApiResponse(code = 404, message = NodeApi.PRIVATE_NODE_NOT_FOUND_MESSAGE),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public void deletePrivateNode(
            @ApiParam(value = "The id of the Node that needs to be deleted",
                      required = true) @PathParam("node_id") final String nodeId)
            throws AuthorizationException,
            NotFoundException,
            InvalidObjectIdException;

    /**
     * Remove a public node, and return it to being an unidentified node
     *
     * @param nodeId the id of the node to remove
     * @throws AuthorizationException If the user is not authorized to delete the public node
     * @throws NotFoundException When the public node that is referred to is not found
     * @throws InvalidObjectIdException When the argument nodeId is an invalid ObjectId
     */
    @DELETE
    @Path("/publicnode/{node_id}")
    @Consumes(MediaType.TEXT_PLAIN)
    @ApiOperation(nickname = "deletePublicNode",
                  value = "Remove a public node",
                  notes = "Remove a public node and make it unidentified again",
                  code = 204,
                  authorizations = {@Authorization(value = OrchestratorApi.ADMIN_AUTHENTICATION)})
    @ApiResponses(value = {@ApiResponse(code = 204, message = "Public node succesfully deleted"),
            @ApiResponse(code = 400, message = InvalidObjectIdException.INVALID_OBJECT_ID_MESSAGE),
            @ApiResponse(code = 404, message = NodeApi.PUBLIC_NODE_NOT_FOUND_MESSAGE),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public void deletePublicNode(
            @ApiParam(value = "The id of the Node that needs to be feleted",
                      required = true) @PathParam("node_id") final String nodeId)
            throws AuthorizationException,
            NotFoundException,
            InvalidObjectIdException;

    /**
     * Get a private node based on its known ObjectId
     *
     * @param nodeId the id of the node to retrieve
     * @return the private node that has the provided nodeId
     * @throws AuthorizationException If the user is not authorized to get the information on the private node
     * @throws NotFoundException When the private node that is referred to is not found
     * @throws InvalidObjectIdException When the argument nodeId is an invalid ObjectId
     */
    @GET
    @Path("/privatenode/{node_id}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "getPrivateNode",
                  value = "Find a private node",
                  notes = "Find the private node with the specified Id",
                  authorizations = {@Authorization(value = OrchestratorApi.USER_AUTHENTICATION)})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The definition of the private node", response = PrivateNode.class),
            @ApiResponse(code = 400, message = InvalidObjectIdException.INVALID_OBJECT_ID_MESSAGE),
            @ApiResponse(code = 404, message = NodeApi.PRIVATE_NODE_NOT_FOUND_MESSAGE),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public PrivateNode getPrivateNode(
            @ApiParam(value = "The id of the Node that needs to be fetched",
                      required = true) @PathParam("node_id") final String nodeId)
            throws AuthorizationException,
            NotFoundException,
            InvalidObjectIdException;

    /**
     * Get a public node based on its known ObjectId
     *
     * @param nodeId the id of the node to retrieve
     * @return the public node that has the provided nodeId
     * @throws AuthorizationException If the user is not authenticated to get the information on the public node
     * @throws NotFoundException When the public node that is referred to is not found
     * @throws InvalidObjectIdException When the argument nodeId is an invalid ObjectId
     */
    @GET
    @Path("/publicnode/{node_id}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "getPublicNode",
                  value = "Find a public node",
                  notes = "Find the public node with the specified Id",
                  authorizations = {@Authorization(value = OrchestratorApi.USER_AUTHENTICATION)})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The definition of the public node", response = PublicNode.class),
            @ApiResponse(code = 400, message = InvalidObjectIdException.INVALID_OBJECT_ID_MESSAGE),
            @ApiResponse(code = 404, message = NodeApi.PUBLIC_NODE_NOT_FOUND_MESSAGE)})
    public PublicNode getPublicNode(
            @ApiParam(value = "The id of the Node that needs to be fetched",
                      required = true) @PathParam("node_id") final String nodeId)
            throws NotFoundException,
            InvalidObjectIdException,
            AuthorizationException;

    /**
     * List all private nodes that the user is authenticated to retrieve. When the user is an administrator, this will
     * return all private nodes; when the user is not an administrator, this will return all private nodes that are
     * registered under his name.
     *
     * @param page the current page to view (defaults to 1)
     * @param perPage the amount of nodes to view per page (defaults to
     *            {@value org.flexiblepower.api.OrchestratorApi#DEFAULT_ITEMS_PER_PAGE})
     * @param sortDir the direction to sort the nodes (defaults to "ASC")
     * @param sortField the field to sort the nodes on (defaults to "id")
     * @param filters a list of filters in JSON notation
     * @return A list of all available private nodes
     * @throws AuthorizationException If the user is not logged in
     */
    @GET
    @Path("/privatenode")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "listPrivateNodes",
                  value = "List private nodes",
                  notes = "List the private nodes of the logged in user",
                  response = PrivateNode.class,
                  responseContainer = "List",
                  authorizations = {@Authorization(value = OrchestratorApi.USER_AUTHENTICATION)})
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                         message = "List of private nodes owned by this user",
                         response = PrivateNode.class,
                         responseContainer = "List"),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public List<PrivateNode> listPrivateNodes(@QueryParam("_page") @DefaultValue("1") int page,
            @QueryParam("_perPage") @DefaultValue(OrchestratorApi.DEFAULT_ITEMS_PER_PAGE) int perPage,
            @QueryParam("_sortDir") @DefaultValue("ASC") String sortDir,
            @QueryParam("_sortField") @DefaultValue("id") String sortField,
            @QueryParam("_filters") @DefaultValue("{}") String filters) throws AuthorizationException;

    /**
     * List all public nodes. Since by design of dEF-Pi all users can run code on public nodes, this function will
     * always return all public nodes, as long as the user is valid.
     *
     * @param page the current page to view (defaults to 1)
     * @param perPage the amount of nodes to view per page (defaults to
     *            {@value org.flexiblepower.api.OrchestratorApi#DEFAULT_ITEMS_PER_PAGE})
     * @param sortDir the direction to sort the nodes (defaults to "ASC")
     * @param sortField the field to sort the nodes on (defaults to "id")
     * @param filters a list of filters in JSON notation
     * @return A list of all public nodes.
     * @throws AuthorizationException If the user is not logged in
     */
    @GET
    @Path("/publicnode")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "listPublicNodes",
                  value = "List public nodes",
                  notes = "List all public nodes",
                  response = PublicNode.class,
                  responseContainer = "List",
                  authorizations = {@Authorization(value = OrchestratorApi.USER_AUTHENTICATION)})
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                         message = "List all public nodes",
                         response = PublicNode.class,
                         responseContainer = "List"),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public List<PublicNode> listPublicNodes(@QueryParam("_page") @DefaultValue("1") int page,
            @QueryParam("_perPage") @DefaultValue(OrchestratorApi.DEFAULT_ITEMS_PER_PAGE) int perPage,
            @QueryParam("_sortDir") @DefaultValue("ASC") String sortDir,
            @QueryParam("_sortField") @DefaultValue("id") String sortField,
            @QueryParam("_filters") @DefaultValue("{}") String filters) throws AuthorizationException;

    /**
     * List all unidentified nodes. By design only administrators may list unidentified nodes.
     *
     * @param page the current page to view (defaults to 1)
     * @param perPage the amount of nodes to view per page (defaults to
     *            {@value org.flexiblepower.api.OrchestratorApi#DEFAULT_ITEMS_PER_PAGE})
     * @param sortDir the direction to sort the nodes (defaults to "ASC")
     * @param sortField the field to sort the nodes on (defaults to "id")
     * @param filters a list of filters in JSON notation
     * @return A list of all unidentified nodes
     * @throws AuthorizationException If the user is logged in as an administrator
     */
    @GET
    @Path("/unidentifiednode")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "listUnidentifiedNodes",
                  value = "List unidentified nodes",
                  notes = "List all unidentified nodes",
                  response = UnidentifiedNode.class,
                  responseContainer = "List",
                  authorizations = {@Authorization(value = OrchestratorApi.ADMIN_AUTHENTICATION)})
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                         message = "List all unidentified nodes",
                         response = UnidentifiedNode.class,
                         responseContainer = "List"),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public List<UnidentifiedNode> listUnidentifiedNodes(@QueryParam("_page") @DefaultValue("1") int page,
            @QueryParam("_perPage") @DefaultValue(OrchestratorApi.DEFAULT_ITEMS_PER_PAGE) int perPage,
            @QueryParam("_sortDir") @DefaultValue("ASC") String sortDir,
            @QueryParam("_sortField") @DefaultValue("id") String sortField,
            @QueryParam("_filters") @DefaultValue("{}") String filters) throws AuthorizationException;

    /**
     * Attempt to create a new node pool with provided specification.
     *
     * @param newNodePool the specification of the new node pool
     * @return the created node pool
     * @throws AuthorizationException if the user is not logged in as an administrator.
     */
    @POST
    @Path("/nodepool")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "createNodePool",
                  value = "Create NodePool",
                  notes = "Create a new NodePool",
                  response = String.class,
                  authorizations = {@Authorization(value = OrchestratorApi.ADMIN_AUTHENTICATION)})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "New NodePool created", response = NodePool.class),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public NodePool
            createNodePool(@ApiParam(value = "The new NodePool to add", required = true) final NodePool newNodePool)
                    throws AuthorizationException;

    /**
     * Attempt to update the information of the node pool with the provided specification.
     *
     * @param nodePoolId the ObjectId of the node pool to update
     * @param updatedNodePool the updated specification of the new node pool
     * @return the updated node pool
     * @throws AuthorizationException if the user is not logged in as an administrator.
     * @throws NodePoolNotFoundException if the node pool could not be found
     * @throws InvalidObjectIdException if the provided ObjectId is not valid
     */
    @PUT
    @Path("/nodepool/{nodepool_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "updateNodePool",
                  value = "Update NodePool",
                  notes = "Update a NodePool",
                  authorizations = {@Authorization(value = OrchestratorApi.ADMIN_AUTHENTICATION)})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "NodePool updated", response = NodePool.class),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE),
            @ApiResponse(code = 400, message = InvalidObjectIdException.INVALID_OBJECT_ID_MESSAGE),
            @ApiResponse(code = 404, message = NodePoolNotFoundException.NODEPOOL_NOT_FOUND_MESSAGE)})
    public NodePool updateNodePool(
            @ApiParam(value = "The id of the NodePool that needs to be updated",
                      required = true) @PathParam("nodepool_id") final String nodePoolId,
            @ApiParam(value = "The NodePool to update", required = true) final NodePool updatedNodePool)
            throws AuthorizationException,
            NodePoolNotFoundException,
            InvalidObjectIdException;

    /**
     * Attempt to delete the node pool with the provided id.
     *
     * @param nodePoolId the ObjectId of the node pool to delete
     * @throws AuthorizationException if the user is not logged in as an administrator.
     * @throws NodePoolNotFoundException if the node pool could not be found
     * @throws InvalidObjectIdException if the provided ObjectId is not valid
     */
    @DELETE
    @Path("/nodepool/{nodepool_id}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "deleteNodePool",
                  value = "Delete NodePool",
                  notes = "Delete the NodePool with the provided Id",
                  code = 204,
                  authorizations = {@Authorization(value = OrchestratorApi.ADMIN_AUTHENTICATION)})
    @ApiResponses(value = {@ApiResponse(code = 204, message = "NodePool deleted"),
            @ApiResponse(code = 400, message = InvalidObjectIdException.INVALID_OBJECT_ID_MESSAGE),
            @ApiResponse(code = 404, message = UserNotFoundException.USER_NOT_FOUND_MESSAGE),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public void deleteNodePool(
            @ApiParam(value = "The id of the NodePool that needs to be deleted",
                      required = true) @PathParam("nodepool_id") final String nodePoolId)
            throws AuthorizationException,
            InvalidObjectIdException,
            NodePoolNotFoundException;

    /**
     * Get information about the node pool with the provided id.
     *
     * @param nodePoolId the ObjectId of the node pool to retrieve
     * @return the node pool with the provided node pool
     * @throws AuthorizationException if the user is not logged in
     * @throws NodePoolNotFoundException if the node pool could not be found
     * @throws InvalidObjectIdException if the provided ObjectId is not valid
     */
    @GET
    @Path("/nodepool/{nodepool_id}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "getNodePool",
                  value = "Get NodePool data",
                  notes = "Get data of the NodePool with the provided Id",
                  authorizations = {@Authorization(value = OrchestratorApi.USER_AUTHENTICATION)})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "NodePool data", response = NodePool.class),
            @ApiResponse(code = 400, message = InvalidObjectIdException.INVALID_OBJECT_ID_MESSAGE),
            @ApiResponse(code = 404, message = NodePoolNotFoundException.NODEPOOL_NOT_FOUND_MESSAGE),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public NodePool getNodePool(
            @ApiParam(value = "The id of the NodePool that needs to be fetched",
                      required = true) @PathParam("nodepool_id") final String nodePoolId)
            throws AuthorizationException,
            InvalidObjectIdException,
            NodePoolNotFoundException;

    /**
     * List all existing node pools. Possible filtered, sorted or a subset when using multiple pages.
     *
     * @param page the current page to view (defaults to 1)
     * @param perPage the amount of pools to view per page (defaults to
     *            {@value org.flexiblepower.api.OrchestratorApi#DEFAULT_ITEMS_PER_PAGE})
     * @param sortDir the direction to sort the pools (defaults to "ASC")
     * @param sortField the field to sort the pools on (defaults to "id")
     * @param filters a list of filters in JSON notation
     * @return A list of node pools that are available in the current dEF-Pi environment
     * @throws AuthorizationException if the user is not logged in.
     */
    @GET
    @Path("/nodepool")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "listNodePools",
                  value = "List NodePools",
                  notes = "List all registered NodePools",
                  authorizations = {@Authorization(value = OrchestratorApi.ADMIN_AUTHENTICATION)})

    @ApiResponses(value = {
            @ApiResponse(code = 200,
                         message = "An array of NodePool",
                         response = NodePool.class,
                         responseContainer = "List"),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public List<NodePool> listNodePools(@QueryParam("_page") @DefaultValue("1") int page,
            @QueryParam("_perPage") @DefaultValue(OrchestratorApi.DEFAULT_ITEMS_PER_PAGE) int perPage,
            @QueryParam("_sortDir") @DefaultValue("ASC") String sortDir,
            @QueryParam("_sortField") @DefaultValue("id") String sortField,
            @QueryParam("_filters") @DefaultValue("{}") String filters) throws AuthorizationException;
}
