package org.flexiblepower.rest;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.lang.NotImplementedException;
import org.bson.types.ObjectId;
import org.flexiblepower.api.NodeApi;
import org.flexiblepower.exceptions.ApiException;
import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.InvalidObjectIdException;
import org.flexiblepower.exceptions.NodePoolNotFoundException;
import org.flexiblepower.exceptions.NotFoundException;
import org.flexiblepower.model.NodePool;
import org.flexiblepower.model.PrivateNode;
import org.flexiblepower.model.PublicNode;
import org.flexiblepower.model.UnidentifiedNode;
import org.flexiblepower.model.User;
import org.flexiblepower.orchestrator.MongoDbConnector;
import org.flexiblepower.orchestrator.NodeManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NodeRestApi extends BaseApi implements NodeApi {

    private final NodeManager nodeManager;

    protected NodeRestApi(@Context final HttpHeaders httpHeaders, @Context final SecurityContext securityContext) {
        super(httpHeaders, securityContext);
        this.nodeManager = NodeManager.getInstance();
    }

    @Override
    public PrivateNode createPrivateNode(final PrivateNode newNode) throws AuthorizationException {
        this.assertUserIsAdmin();
        final UnidentifiedNode un = this.nodeManager.getUnidentifiedNodeByDockerId(newNode.getDockerId());
        if (un == null) {
            throw new ApiException(404, "Node could not be found");
        }
        final User user = this.db.getUser(newNode.getUserId()); // TODO use user manager
        if (user == null) {
            throw new ApiException(404, "User could not be found");
        }
        NodeRestApi.log.info("Making node " + newNode.getDockerId() + " into a private node");
        return this.nodeManager.makeUnidentifiedNodePrivate(un, user);
    }

    @Override
    public PublicNode createPublicNode(final PublicNode newNode) {
        this.assertUserIsAdmin();
        final UnidentifiedNode un = this.nodeManager.getUnidentifiedNodeByDockerId(newNode.getDockerId());
        if (un == null) {
            throw new ApiException(404, "Node could not be found");
        }
        final NodePool nodePool = this.nodeManager.getNodePool(newNode.getNodePoolId());
        if (nodePool == null) {
            throw new ApiException(404, "NodePool could not be found");
        }
        NodeRestApi.log.info("Making node " + newNode.getDockerId() + " into a public node");
        return this.nodeManager.makeUnidentifiedNodePublic(un, nodePool);
    }

    @Override
    public void deletePrivateNode(final String nodeId) throws InvalidObjectIdException {
        this.assertUserIsAdmin();
        throw new NotImplementedException(); // TODO
    }

    @Override
    public void deletePublicNode(final String nodeId) throws InvalidObjectIdException {
        this.assertUserIsAdmin();
        throw new NotImplementedException(); // TODO
    }

    @Override
    public PrivateNode getPrivateNode(final String nodeId) throws InvalidObjectIdException {
        this.assertUserIsAdmin();
        return this.nodeManager.getPrivateNode(new ObjectId(nodeId));
    }

    @Override
    public PublicNode getPublicNode(final String nodeId) throws InvalidObjectIdException {
        this.assertUserIsAdmin();
        return this.nodeManager.getPublicNode(new ObjectId(nodeId));
    }

    @Override
    public List<PrivateNode> listPrivateNodes() {
        this.assertUserIsAdmin();
        return this.nodeManager.getPrivateNodes();
    }

    @Override
    public List<PublicNode> listPublicNodes() {
        this.assertUserIsAdmin();
        return this.nodeManager.getPublicNodes();
    }

    @Override
    public List<UnidentifiedNode> listUnidentifiedNodes() {
        this.assertUserIsAdmin();
        return this.nodeManager.getUnidentifiedNodes();
    }

    @Override
    public NodePool createNodePool(final NodePool newNodePool) throws AuthorizationException {
        this.assertUserIsAdmin();
        return this.nodeManager.saveNodePool(newNodePool);
    }

    @Override
    public NodePool updateNodePool(final String nodePoolId, final NodePool updatedNodePool)
            throws AuthorizationException, NodePoolNotFoundException {
        if ((nodePoolId == null) || !nodePoolId.equals(updatedNodePool.getId().toString())) {
            throw new ApiException(403, "Invalid id");
        }
        final NodePool nodePool = this.nodeManager.getNodePool(new ObjectId(nodePoolId));
        if (nodePool == null) {
            throw new NodePoolNotFoundException();
        }
        return this.nodeManager.saveNodePool(updatedNodePool);
    }

    @Override
    public void deleteNodePool(final String nodePoolId)
            throws AuthorizationException, InvalidObjectIdException, NotFoundException {
        final NodePool nodePool = this.nodeManager.getNodePool(new ObjectId(nodePoolId));
        if (nodePool == null) {
            throw new NodePoolNotFoundException();
        }
        this.nodeManager.deleteNodePool(nodePool);
    }

    @Override
    public NodePool getNodePool(final String nodePoolId)
            throws AuthorizationException, InvalidObjectIdException, NotFoundException {
        final NodePool nodePool = this.nodeManager.getNodePool(new ObjectId(nodePoolId));
        if (nodePool == null) {
            throw new NodePoolNotFoundException();
        }
        return nodePool;
    }

    @Override
    public Response listNodePools(final int page,
            final int perPage,
            final String sortDir,
            final String sortField,
            final String filters) throws AuthorizationException {
        final Map<String, Object> filter = MongoDbConnector.parseFilters(filters);
        return Response.status(Status.OK.getStatusCode())
                .header("X-Total-Count", Integer.toString(this.nodeManager.countNodePools(filter)))
                .entity(this.nodeManager.listNodePools(page, perPage, sortDir, sortField, filter))
                .build();
    }
}
