package org.flexiblepower.api;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
@Produces(MediaType.APPLICATION_JSON)
public interface NodeApi {

    @POST
    @Path("/private")
    @ApiOperation(value = "Create a new private node",
                  notes = "Create a private node based on the id of an unidentified node",
                  authorizations = {@Authorization(value = "AdminSecurity")})
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Private node succesfully created", response = String.class),
            @ApiResponse(code = 404, message = "Unidentified node not found", response = void.class)})
    public String createPrivateNode(final PrivateNode newNode);

    @POST
    @Path("/public")
    @ApiOperation(value = "Create a new public node",
                  notes = "Create a public node based on the id of an unidentified node",
                  authorizations = {@Authorization(value = "AdminSecurity")})
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Public node succesfully created", response = String.class),
            @ApiResponse(code = 404, message = "Unidentified node not found", response = void.class)})
    public String createPublicNode(final PublicNode newNode);

    @DELETE
    @Path("/private/{node_id}")
    @ApiOperation(value = "Remove a private node",
                  notes = "Remove a private node and make it unidentified again",
                  authorizations = {@Authorization(value = "AdminSecurity")})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Private node succesfully deleted", response = void.class),
            @ApiResponse(code = 404, message = "Private node not found", response = void.class)})
    public void
            deletePrivateNode(
                    @ApiParam(value = "The id of the Node that needs to be deleted",
                              required = true) @PathParam("node_id") final String nodeId)
                    throws InvalidObjectIdException;

    @DELETE
    @Path("/public/{node_id}")
    @ApiOperation(value = "Remove a public node",
                  notes = "Remove a public node and make it unidentified again",
                  authorizations = {@Authorization(value = "AdminSecurity")})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Public node succesfully deleted", response = void.class),
            @ApiResponse(code = 404, message = "Public node not found", response = void.class)})
    public void
            deletePublicNode(
                    @ApiParam(value = "The id of the Node that needs to be feleted",
                              required = true) @PathParam("node_id") final String nodeId)
                    throws InvalidObjectIdException;

    @GET
    @Path("/private/{node_id}")
    @ApiOperation(value = "Find a private node",
                  notes = "Find the private node with the specified Id",
                  authorizations = {@Authorization(value = "UserSecurity")})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "List all private nodes", response = PrivateNode.class)})
    public PrivateNode
            getPrivateNode(
                    @ApiParam(value = "The id of the Node that needs to be fetched",
                              required = true) @PathParam("node_id") final String nodeId)
                    throws InvalidObjectIdException;

    @GET
    @Path("/public/{node_id}")
    @ApiOperation(value = "Find a public node",
                  notes = "Find the public node with the specified Id",
                  authorizations = {@Authorization(value = "UserSecurity")})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "List all public nodes", response = PublicNode.class)})
    public PublicNode
            getPublicNode(
                    @ApiParam(value = "The id of the Node that needs to be fetched",
                              required = true) @PathParam("node_id") final String nodeId)
                    throws InvalidObjectIdException;

    @GET
    @Path("/private")
    @ApiOperation(value = "List private nodes",
                  notes = "List the private nodes of the logged in user",
                  response = PrivateNode.class,
                  responseContainer = "List",
                  authorizations = {@Authorization(value = "UserSecurity")})
    @ApiResponses(value = {@ApiResponse(code = 200,
                                        message = "List of private nodes owned by this user",
                                        response = PrivateNode.class,
                                        responseContainer = "List")})
    public List<PrivateNode> listPrivateNodes();

    @GET
    @Path("/public")
    @ApiOperation(value = "List public nodes",
                  notes = "List all public nodes",
                  response = PublicNode.class,
                  responseContainer = "List",
                  authorizations = {@Authorization(value = "UserSecurity")})
    @ApiResponses(value = {@ApiResponse(code = 200,
                                        message = "List all public nodes",
                                        response = PublicNode.class,
                                        responseContainer = "List")})
    public List<PublicNode> listPublicNodes();

    @GET
    @Path("/unidentified")
    @ApiOperation(value = "List unidentified nodes",
                  notes = "List all unidentified nodes",
                  response = UnidentifiedNode.class,
                  responseContainer = "List",
                  authorizations = {@Authorization(value = "AdminSecurity")})
    @ApiResponses(value = {@ApiResponse(code = 200,
                                        message = "List all unidentified nodes",
                                        response = UnidentifiedNode.class,
                                        responseContainer = "List")})
    public List<UnidentifiedNode> listUnidentifiedNodes();
}
