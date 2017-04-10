package org.flexiblepower.api;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.InvalidObjectIdException;
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
@Path("node")
public interface NodeApi {

    final static String PRIVATE_NODE_NOT_FOUND_MESSAGE = "Private node not found";
    final static String PUBLIC_NODE_NOT_FOUND_MESSAGE = "Public node not found";
    final static String UNIDENTIFIED_NODE_NOT_FOUND_MESSAGE = "Unidentified node not found";

    @POST
    @Path("/private")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "createPrivateNode",
                  value = "Create a new private node",
                  notes = "Create a private node based on the id of an unidentified node",
                  authorizations = {@Authorization(value = OrchestratorApi.ADMIN_AUTHENTICATION)})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Private node succesfully created", response = String.class),
            @ApiResponse(code = 404, message = NodeApi.UNIDENTIFIED_NODE_NOT_FOUND_MESSAGE),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public String createPrivateNode(
            @ApiParam(name = "newNode",
                      value = "The definition of the node to create",
                      required = true) final PrivateNode newNode)
            throws AuthorizationException,
            NotFoundException;

    @POST
    @Path("/public")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "createPublicNode",
                  value = "Create a new public node",
                  notes = "Create a public node based on the id of an unidentified node",
                  authorizations = {@Authorization(value = OrchestratorApi.ADMIN_AUTHENTICATION)})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Public node succesfully created", response = String.class),
            @ApiResponse(code = 404, message = NodeApi.UNIDENTIFIED_NODE_NOT_FOUND_MESSAGE),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public String createPublicNode(
            @ApiParam(name = "newNode",
                      value = "The definition of the node to create",
                      required = true) final PublicNode newNode)
            throws AuthorizationException,
            NotFoundException;

    @DELETE
    @Path("/private/{node_id}")
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

    @DELETE
    @Path("/public/{node_id}")
    @ApiOperation(nickname = "deletePublicNode",
                  value = "Remove a public node",
                  notes = "Remove a public node and make it unidentified again",
                  code = 204,
                  authorizations = {@Authorization(value = OrchestratorApi.ADMIN_AUTHENTICATION)})
    @ApiResponses(value = {@ApiResponse(code = 204, message = "Public node succesfully deleted"),
            @ApiResponse(code = 400, message = InvalidObjectIdException.INVALID_OBJECT_ID_MESSAGE),
            @ApiResponse(code = 404, message = NodeApi.PUBLIC_NODE_NOT_FOUND_MESSAGE),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public void

            deletePublicNode(
                    @ApiParam(value = "The id of the Node that needs to be feleted",
                              required = true) @PathParam("node_id") final String nodeId)
                    throws AuthorizationException,
                    NotFoundException,
                    InvalidObjectIdException;

    @GET
    @Path("/private/{node_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "getPrivateNode",
                  value = "Find a private node",
                  notes = "Find the private node with the specified Id",
                  authorizations = {@Authorization(value = OrchestratorApi.USER_AUTHENTICATION)})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The definition of the private node", response = PrivateNode.class),
            @ApiResponse(code = 400, message = InvalidObjectIdException.INVALID_OBJECT_ID_MESSAGE),
            @ApiResponse(code = 404, message = NodeApi.PRIVATE_NODE_NOT_FOUND_MESSAGE)})
    public PrivateNode getPrivateNode(
            @ApiParam(value = "The id of the Node that needs to be fetched",
                      required = true) @PathParam("node_id") final String nodeId)
            throws NotFoundException,
            InvalidObjectIdException;

    @GET
    @Path("/public/{node_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "getPublicNode",
                  value = "Find a public node",
                  notes = "Find the public node with the specified Id",
                  authorizations = {@Authorization(value = OrchestratorApi.USER_AUTHENTICATION)})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "List all public nodes", response = PublicNode.class),
            @ApiResponse(code = 404, message = NodeApi.PUBLIC_NODE_NOT_FOUND_MESSAGE),
            @ApiResponse(code = 405, message = AuthorizationException.UNAUTHORIZED_MESSAGE)})
    public PublicNode getPublicNode(
            @ApiParam(value = "The id of the Node that needs to be fetched",
                      required = true) @PathParam("node_id") final String nodeId)
            throws NotFoundException,
            InvalidObjectIdException;

    @GET
    @Path("/private")
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
    public List<PrivateNode> listPrivateNodes();

    @GET
    @Path("/public")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(nickname = "listPublicNodes",
                  value = "List public nodes",
                  notes = "List all public nodes",
                  response = PublicNode.class,
                  responseContainer = "List",
                  authorizations = {@Authorization(value = OrchestratorApi.USER_AUTHENTICATION)})
    @ApiResponses(value = {@ApiResponse(code = 200,
                                        message = "List all public nodes",
                                        response = PublicNode.class,
                                        responseContainer = "List")})
    public List<PublicNode> listPublicNodes();

    @GET
    @Path("/unidentified")
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
}
