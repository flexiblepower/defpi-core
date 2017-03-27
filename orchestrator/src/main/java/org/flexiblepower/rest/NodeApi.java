package org.flexiblepower.rest;

import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import org.flexiblepower.model.PrivateNode;
import org.flexiblepower.model.PublicNode;
import org.flexiblepower.model.UnidentifiedNode;
import org.flexiblepower.orchestrator.DockerConnector;
import org.flexiblepower.orchestrator.MongoDbConnector;

import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.swarm.Node;

import lombok.extern.slf4j.Slf4j;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Slf4j
@Path("node")
@Produces(MediaType.APPLICATION_JSON)
@io.swagger.annotations.Api(description = "the node API")
public class NodeApi {

    private final MongoDbConnector db = new MongoDbConnector();

    private void initUser(final HttpHeaders httpHeaders) {
        final String username = httpHeaders.getHeaderString("username");
        final String password = httpHeaders.getHeaderString("password");
        this.db.setApplicationUser(this.db.getUser(username, password));
    }

    @POST
    @Path("/private")
    @ApiOperation(value = "Create a private node based on the id of an unidentified node",
                  notes = "",
                  response = PrivateNode.class,
                  authorizations = {@io.swagger.annotations.Authorization(value = "AdminSecurity")},
                  tags = {"Node"})
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Private node succesfully created", response = PrivateNode.class),
            @ApiResponse(code = 404, message = "Unidentified node not found", response = PrivateNode.class)})
    public PrivateNode createPrivateNode(@Context final SecurityContext securityContext, final PrivateNode newNode) {
        this.db.insertNode(newNode);
        return newNode;
    }

    @POST
    @Path("/public")
    @ApiOperation(value = "Create a public node based on the id of an unidentified node",
                  notes = "",
                  response = PublicNode.class,
                  authorizations = {@Authorization(value = "AdminSecurity")},
                  tags = {"Node"})
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Public node succesfully created", response = PublicNode.class),
            @ApiResponse(code = 404, message = "Unidentified node not found", response = PublicNode.class)})
    public PublicNode createPublicNode(@Context final SecurityContext securityContext, final PublicNode newNode) {
        this.db.insertNode(newNode);
        return newNode;
    }

    @DELETE
    @Path("/private/{node_id}")
    @ApiOperation(value = "Remove a node (and make it unidentified again)",
                  notes = "",
                  response = void.class,
                  authorizations = {@Authorization(value = "AdminSecurity")},
                  tags = {"Node"})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Private node succesfully deleted", response = void.class),
            @ApiResponse(code = 404, message = "Private node not found", response = void.class)})
    public void deletePrivateNode(
            @ApiParam(value = "The id of the Node that needs to be deleted",
                      required = true) @PathParam("node_id") final String nodeId,
            @Context final SecurityContext securityContext) {
        this.db.removeNode(nodeId);
    }

    @DELETE
    @Path("/public/{node_id}")
    @ApiOperation(value = "Remove a node (and make it unidentified again)",
                  notes = "",
                  response = void.class,
                  authorizations = {@Authorization(value = "AdminSecurity")},
                  tags = {"Node",})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Public node succesfully deleted", response = void.class),
            @ApiResponse(code = 404, message = "Public node not found", response = void.class)})
    public void deletePublicNode(
            @ApiParam(value = "The id of the Node that needs to be feleted",
                      required = true) @PathParam("node_id") final String nodeId,
            @Context final SecurityContext securityContext) {
        this.db.removeNode(nodeId);
    }

    @GET
    @Path("/private/{node_id}")
    @ApiOperation(value = "Find the private node with the specified Id",
                  notes = "",
                  response = PrivateNode.class,
                  authorizations = {@Authorization(value = "UserSecurity")},
                  tags = {"Node"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "List all private nodes", response = PrivateNode.class)})
    public PrivateNode getPrivateNode(
            @ApiParam(value = "The id of the Node that needs to be fetched",
                      required = true) @PathParam("node_id") final String nodeId,
            @Context final SecurityContext securityContext) {
        return this.db.getPrivateNode(nodeId);
    }

    @GET
    @Path("/public/{node_id}")
    @ApiOperation(value = "Find the public node with the specified Id",
                  notes = "",
                  response = PublicNode.class,
                  authorizations = {@Authorization(value = "UserSecurity")},
                  tags = {"Node"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "List all public nodes", response = PublicNode.class)})
    public PublicNode getPublicNode(
            @ApiParam(value = "The id of the Node that needs to be fetched",
                      required = true) @PathParam("node_id") final String nodeId,
            @Context final SecurityContext securityContext) {
        return this.db.getPublicNode(nodeId);
    }

    @GET
    @Path("/private")
    @ApiOperation(value = "Lists the private nodes of the current user",
                  notes = "",
                  response = PrivateNode.class,
                  responseContainer = "List",
                  authorizations = {@Authorization(value = "UserSecurity")},
                  tags = {"Node"})
    @ApiResponses(value = {@ApiResponse(code = 200,
                                        message = "List of private nodes owned by this user",
                                        response = PrivateNode.class,
                                        responseContainer = "List")})
    public List<PrivateNode> listPrivateNodes(@Context final SecurityContext securityContext) {
        return this.db.getPrivateNodes();
    }

    @GET
    @Path("/public")
    @ApiOperation(value = "Lists all public nodes",
                  notes = "",
                  response = PublicNode.class,
                  responseContainer = "List",
                  authorizations = {@Authorization(value = "UserSecurity")},
                  tags = {"Node"})
    @ApiResponses(value = {@ApiResponse(code = 200,
                                        message = "List all public nodes",
                                        response = PublicNode.class,
                                        responseContainer = "List")})
    public List<PublicNode> listPublicNodes(@Context final SecurityContext securityContext) {
        return this.db.getPublicNodes();
    }

    @GET
    @Path("/unidentified")
    @ApiOperation(value = "Lists all public nodes",
                  notes = "",
                  response = UnidentifiedNode.class,
                  responseContainer = "List",
                  authorizations = {@Authorization(value = "AdminSecurity")},
                  tags = {"Node"})
    @ApiResponses(value = {@ApiResponse(code = 200,
                                        message = "List all unidentified nodes",
                                        response = UnidentifiedNode.class,
                                        responseContainer = "List")})
    public List<UnidentifiedNode> listUnidentifiedNodes(@Context final SecurityContext securityContext) {
        try {
            final List<Node> nodeList = DockerConnector.init().listNodes();
            final LinkedList<UnidentifiedNode> ret = new LinkedList<>();
            for (final Node node : nodeList) {
                if ((this.db.getPublicNode(node.id()) == null) && (this.db.getPrivateNode(node.id()) == null)) {
                    ret.add(new UnidentifiedNode(node.description().hostname()));
                }
            }
            return ret;
        } catch (DockerException | InterruptedException | DockerCertificateException e) {
            e.printStackTrace();
            throw new ApiException(e);
        }

    }
}