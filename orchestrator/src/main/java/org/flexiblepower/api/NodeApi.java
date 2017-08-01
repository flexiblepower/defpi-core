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
import javax.ws.rs.core.Response;

import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.InvalidObjectIdException;
import org.flexiblepower.exceptions.NodePoolNotFoundException;
import org.flexiblepower.exceptions.NotFoundException;
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

@Api("Node")
@Path("/")
public interface NodeApi {

    final static String PRIVATE_NODE_NOT_FOUND_MESSAGE = "Private node not found";
    final static String PUBLIC_NODE_NOT_FOUND_MESSAGE = "Public node not found";
    final static String UNIDENTIFIED_NODE_NOT_FOUND_MESSAGE = "Unidentified node not found";
    final static String NODE_POOL_NOT_FOUND_MESSAGE = "Node pool not found";

    @POST
    @Path("/privatenode")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "createPrivateNode",
                  value = "Create a new private node",
                  notes = "Create a private node based on the id of an unidentified node",
                  authorizations = {@Authorization(value = OrchestratorApi.ADMIN_AUTHENTICATION)})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Private node succesfully created", response = String.class),
            @ApiResponse(code = 400, message = InvalidObjectIdException.INVALID_OBJECT_ID_MESSAGE),
            @ApiResponse(code = 404, message = NodeApi.UNIDENTIFIED_NODE_NOT_FOUND_MESSAGE),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public PrivateNode createPrivateNode(
            @ApiParam(name = "newNode",
                      value = "The definition of the node to create",
                      required = true) final PrivateNode newNode)
            throws AuthorizationException, NotFoundException, InvalidObjectIdException;

    @POST
    @Path("/publicnode")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "createPublicNode",
                  value = "Create a new public node",
                  notes = "Create a public node based on the id of an unidentified node",
                  authorizations = {@Authorization(value = OrchestratorApi.ADMIN_AUTHENTICATION)})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Public node succesfully created", response = String.class),
            @ApiResponse(code = 400, message = InvalidObjectIdException.INVALID_OBJECT_ID_MESSAGE),
            @ApiResponse(code = 404, message = NodeApi.UNIDENTIFIED_NODE_NOT_FOUND_MESSAGE),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public PublicNode createPublicNode(
            @ApiParam(name = "newNode",
                      value = "The definition of the node to create",
                      required = true) final PublicNode newNode)
            throws AuthorizationException, NotFoundException, InvalidObjectIdException;

    @DELETE
    @Path("/privatenode/{node_id}")
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
            throws AuthorizationException, NotFoundException, InvalidObjectIdException;

    @DELETE
    @Path("/publicnode/{node_id}")
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
            throws AuthorizationException, NotFoundException, InvalidObjectIdException;

    @GET
    @Path("/privatenode/{node_id}")
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
            throws AuthorizationException, NotFoundException, InvalidObjectIdException;

    @GET
    @Path("/publicnode/{node_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "getPublicNode",
                  value = "Find a public node",
                  notes = "Find the public node with the specified Id",
                  authorizations = {@Authorization(value = OrchestratorApi.USER_AUTHENTICATION)})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "List all public nodes", response = PublicNode.class),
            @ApiResponse(code = 400, message = InvalidObjectIdException.INVALID_OBJECT_ID_MESSAGE),
            @ApiResponse(code = 404, message = NodeApi.PUBLIC_NODE_NOT_FOUND_MESSAGE)})
    public PublicNode getPublicNode(
            @ApiParam(value = "The id of the Node that needs to be fetched",
                      required = true) @PathParam("node_id") final String nodeId)
            throws NotFoundException, InvalidObjectIdException, AuthorizationException;

    @GET
    @Path("/privatenode")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "listPrivateNodes",
                  value = "List private nodes",
                  notes = "List the private nodes of the logged in user",
                  response = PrivateNode.class,
                  responseContainer = "List",
                  authorizations = {@Authorization(value = OrchestratorApi.USER_AUTHENTICATION)})
    @ApiResponses(value = {@ApiResponse(code = 200,
                                        message = "List of private nodes owned by this user",
                                        response = PrivateNode.class,
                                        responseContainer = "List")})
    public List<PrivateNode> listPrivateNodes() throws AuthorizationException;

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
                         responseContainer = "List")})
    public List<PublicNode> listPublicNodes() throws AuthorizationException;

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
    public List<UnidentifiedNode> listUnidentifiedNodes() throws AuthorizationException;

    @POST
    @Path("/nodepool")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "createNodePool",
                  value = "Create NodePool",
                  notes = "Create a new NodePool",
                  response = String.class,
                  authorizations = {@Authorization(value = OrchestratorApi.ADMIN_AUTHENTICATION)})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "New NodePool created", response = String.class),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public NodePool
            createNodePool(@ApiParam(value = "The new NodePool to add", required = true) final NodePool newNodePool)
                    throws AuthorizationException;

    @PUT
    @Path("/nodepool/{nodepool_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "updateNodePool",
                  value = "Update NodePool",
                  notes = "Update a NodePool",
                  response = String.class,
                  authorizations = {@Authorization(value = OrchestratorApi.ADMIN_AUTHENTICATION)})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "NodePool updated", response = String.class),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public NodePool updateNodePool(
            @ApiParam(value = "The id of the NodePool that needs to be deleted",
                      required = true) @PathParam("nodepool_id") final String nodePoolId,
            @ApiParam(value = "The NodePool to update", required = true) final NodePool updatedNodePool)
            throws AuthorizationException, NodePoolNotFoundException, InvalidObjectIdException;

    @DELETE
    @Path("/nodepool/{nodepool_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "deleteNodePool",
                  value = "Delete NodePool",
                  notes = "Delete the NodePool with the provided Id",
                  code = 204,
                  authorizations = {@Authorization(value = OrchestratorApi.ADMIN_AUTHENTICATION)})
    @ApiResponses(value = {@ApiResponse(code = 204, message = "NodePool deleted"),
            @ApiResponse(code = 400, message = InvalidObjectIdException.INVALID_OBJECT_ID_MESSAGE),
            @ApiResponse(code = 404, message = UserApi.USER_NOT_FOUND_MESSAGE),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public void deleteNodePool(
            @ApiParam(value = "The id of the NodePool that needs to be deleted",
                      required = true) @PathParam("nodepool_id") final String nodePoolId)
            throws AuthorizationException, InvalidObjectIdException, NotFoundException;

    @GET
    @Path("/nodepool/{nodepool_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "getNodePool",
                  value = "Get NodePool data",
                  notes = "Get data of the NodePool with the provided Id",
                  authorizations = {@Authorization(value = OrchestratorApi.USER_AUTHENTICATION)})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "NodePool data", response = NodePool.class),
            @ApiResponse(code = 400, message = InvalidObjectIdException.INVALID_OBJECT_ID_MESSAGE),
            @ApiResponse(code = 404, message = NodeApi.NODE_POOL_NOT_FOUND_MESSAGE),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public NodePool getNodePool(
            @ApiParam(value = "The id of the NodePool that needs to be fetched",
                      required = true) @PathParam("nodepool_id") final String nodePoolId)
            throws AuthorizationException, InvalidObjectIdException, NotFoundException;

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
    public Response listNodePools(@QueryParam("_page") @DefaultValue("1") int page,
            @QueryParam("_perPage") @DefaultValue("1000") int perPage,
            @QueryParam("_sortDir") @DefaultValue("ASC") String sortDir,
            @QueryParam("_sortField") @DefaultValue("id") String sortField,
            @QueryParam("_filters") @DefaultValue("{}") String filters) throws AuthorizationException;
}
