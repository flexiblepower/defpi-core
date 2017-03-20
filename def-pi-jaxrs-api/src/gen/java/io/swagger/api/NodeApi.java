package io.swagger.api;

import io.swagger.model.*;
import io.swagger.api.NodeApiService;
import io.swagger.api.factories.NodeApiServiceFactory;

import io.swagger.annotations.ApiParam;
import io.swagger.jaxrs.*;

import io.swagger.model.PrivateNode;
import io.swagger.model.PublicNode;
import io.swagger.model.UnidentifiedNode;

import java.util.List;
import io.swagger.api.NotFoundException;

import java.io.InputStream;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.*;
import javax.validation.constraints.*;

@Path("/node")

@Produces({ "application/json" })
@io.swagger.annotations.Api(description = "the node API")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2017-03-20T14:11:17.160Z")
public class NodeApi  {
   private final NodeApiService delegate = NodeApiServiceFactory.getNodeApi();

    @POST
    @Path("/private")
    
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Create a private node based on the id of an unidentified node", notes = "", response = PrivateNode.class, authorizations = {
        @io.swagger.annotations.Authorization(value = "AdminSecurity")
    }, tags={ "Node", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 201, message = "Private node succesfully created", response = PrivateNode.class),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "Unidentified node not found", response = PrivateNode.class) })
    public Response createPrivateNode(@Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.createPrivateNode(securityContext);
    }
    @POST
    @Path("/public")
    
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Create a public node based on the id of an unidentified node", notes = "", response = PublicNode.class, authorizations = {
        @io.swagger.annotations.Authorization(value = "AdminSecurity")
    }, tags={ "Node", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 201, message = "Public node succesfully created", response = PublicNode.class),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "Unidentified node not found", response = PublicNode.class) })
    public Response createPublicNode(@Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.createPublicNode(securityContext);
    }
    @DELETE
    @Path("/private/{node_id}")
    
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Remove a node (and make it unidentified again)", notes = "", response = void.class, authorizations = {
        @io.swagger.annotations.Authorization(value = "AdminSecurity")
    }, tags={ "Node", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "Private node succesfully deleted", response = void.class),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "Private node not found", response = void.class) })
    public Response deletePriivateNode(@ApiParam(value = "The id of the Node that needs to be feleted",required=true) @PathParam("node_id") String nodeId
,@Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.deletePriivateNode(nodeId,securityContext);
    }
    @DELETE
    @Path("/public/{node_id}")
    
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Remove a node (and make it unidentified again)", notes = "", response = void.class, authorizations = {
        @io.swagger.annotations.Authorization(value = "AdminSecurity")
    }, tags={ "Node", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "Public node succesfully deleted", response = void.class),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "Public node not found", response = void.class) })
    public Response deletePublicNode(@ApiParam(value = "The id of the Node that needs to be feleted",required=true) @PathParam("node_id") String nodeId
,@Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.deletePublicNode(nodeId,securityContext);
    }
    @GET
    @Path("/private/{node_id}")
    
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Lists all private nodes", notes = "", response = PrivateNode.class, authorizations = {
        @io.swagger.annotations.Authorization(value = "UserSecurity")
    }, tags={ "Node", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "List all private nodes", response = PrivateNode.class) })
    public Response getPrivateNode(@ApiParam(value = "The id of the Node that needs to be fetched",required=true) @PathParam("node_id") String nodeId
,@Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.getPrivateNode(nodeId,securityContext);
    }
    @GET
    @Path("/public/{node_id}")
    
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Lists all public nodes", notes = "", response = PublicNode.class, authorizations = {
        @io.swagger.annotations.Authorization(value = "UserSecurity")
    }, tags={ "Node", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "List all public nodes", response = PublicNode.class) })
    public Response getPublicNode(@ApiParam(value = "The id of the Node that needs to be fetched",required=true) @PathParam("node_id") String nodeId
,@Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.getPublicNode(nodeId,securityContext);
    }
    @GET
    @Path("/private")
    
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Lists the private nodes of the current user", notes = "", response = PrivateNode.class, responseContainer = "List", authorizations = {
        @io.swagger.annotations.Authorization(value = "UserSecurity")
    }, tags={ "Node", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "List of private nodes owned by this user", response = PrivateNode.class, responseContainer = "List") })
    public Response listPrivateNodes(@Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.listPrivateNodes(securityContext);
    }
    @GET
    @Path("/public")
    
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Lists all public nodes", notes = "", response = PublicNode.class, responseContainer = "List", authorizations = {
        @io.swagger.annotations.Authorization(value = "UserSecurity")
    }, tags={ "Node", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "List all public nodes", response = PublicNode.class, responseContainer = "List") })
    public Response listPublicNodes(@Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.listPublicNodes(securityContext);
    }
    @GET
    @Path("/unidentified")
    
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Lists all public nodes", notes = "", response = UnidentifiedNode.class, responseContainer = "List", authorizations = {
        @io.swagger.annotations.Authorization(value = "AdminSecurity")
    }, tags={ "Node", })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "List all unidentified nodes", response = UnidentifiedNode.class, responseContainer = "List") })
    public Response listUnidentifiedNodes(@Context SecurityContext securityContext)
    throws NotFoundException {
        return delegate.listUnidentifiedNodes(securityContext);
    }
}
