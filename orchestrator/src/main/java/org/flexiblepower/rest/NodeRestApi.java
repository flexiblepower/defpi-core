package org.flexiblepower.rest;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.bson.types.ObjectId;
import org.flexiblepower.api.NodeApi;
import org.flexiblepower.api.UserApi;
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
import org.flexiblepower.orchestrator.UserManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NodeRestApi extends BaseApi implements NodeApi {

    private final NodeManager nodeManager = NodeManager.getInstance();
    private final UserManager userManager = UserManager.getInstance();

    protected NodeRestApi(@Context final HttpHeaders httpHeaders) {
        super(httpHeaders);
    }

    @Override
    public PrivateNode createPrivateNode(final PrivateNode newNode) throws AuthorizationException,
            InvalidObjectIdException {
        this.assertUserIsAdmin();
        // TODO this is a hack. The UI gives the id of the Unidentified node, not of the dockerId...
        final ObjectId oid = MongoDbConnector.stringToObjectId(newNode.getDockerId());
        final UnidentifiedNode un = this.nodeManager.getUnidentifiedNode(oid);
        if (un == null) {
            throw new ApiException(Status.NOT_FOUND, NodeApi.UNIDENTIFIED_NODE_NOT_FOUND_MESSAGE);
        }

        final User owner = this.userManager.getUser(newNode.getUserId());
        if (owner == null) {
            throw new ApiException(Status.NOT_FOUND, UserApi.USER_NOT_FOUND_MESSAGE);
        }

        NodeRestApi.log.info("Making node " + newNode.getDockerId() + " into a private node");
        return this.nodeManager.makeUnidentifiedNodePrivate(un, owner);
    }

    @Override
    public PublicNode createPublicNode(final PublicNode newNode) throws AuthorizationException,
            InvalidObjectIdException {
        this.assertUserIsAdmin();
        // TODO this is a hack. The UI gives the id of the Unidentified node, not of the dockerId...
        final ObjectId oid = MongoDbConnector.stringToObjectId(newNode.getDockerId());
        final UnidentifiedNode un = this.nodeManager.getUnidentifiedNode(oid);

        if (un == null) {
            throw new ApiException(Status.NOT_FOUND, NodeApi.UNIDENTIFIED_NODE_NOT_FOUND_MESSAGE);
        }

        final NodePool nodePool = this.nodeManager.getNodePool(newNode.getNodePoolId());
        if (nodePool == null) {
            throw new ApiException(Status.NOT_FOUND, NodeApi.NODE_POOL_NOT_FOUND_MESSAGE);
        }

        NodeRestApi.log.info("Making node " + newNode.getDockerId() + " into a public node");
        return this.nodeManager.makeUnidentifiedNodePublic(un, nodePool);
    }

    @Override
    public void deletePrivateNode(final String nodeId) throws AuthorizationException, InvalidObjectIdException {
        this.assertUserIsAdmin();

        final ObjectId oid = MongoDbConnector.stringToObjectId(nodeId);
        final PrivateNode privateNode = this.nodeManager.getPrivateNode(oid);
        if (privateNode == null) {
            throw new ApiException(Status.NOT_FOUND, NodeApi.PRIVATE_NODE_NOT_FOUND_MESSAGE);
        }
        this.nodeManager.deletePrivateNode(privateNode);
    }

    @Override
    public void deletePublicNode(final String nodeId) throws AuthorizationException, InvalidObjectIdException {
        this.assertUserIsAdmin();

        final ObjectId oid = MongoDbConnector.stringToObjectId(nodeId);
        final PublicNode publicNode = this.nodeManager.getPublicNode(oid);

        if (publicNode == null) {
            throw new ApiException(Status.NOT_FOUND, NodeApi.PUBLIC_NODE_NOT_FOUND_MESSAGE);
        }
        this.nodeManager.deletePublicNode(publicNode);
    }

    @Override
    public PrivateNode getPrivateNode(final String id) throws AuthorizationException, InvalidObjectIdException {
        final ObjectId nodeId = MongoDbConnector.stringToObjectId(id);
        final PrivateNode ret = this.nodeManager.getPrivateNode(nodeId);

        if (ret == null) {
            throw new ApiException(Status.NOT_FOUND, NodeApi.PRIVATE_NODE_NOT_FOUND_MESSAGE);
        }

        this.assertUserIsAdminOrEquals(ret.getUserId());
        return ret;
    }

    @Override
    public PublicNode getPublicNode(final String nodeId) throws InvalidObjectIdException {
        final PublicNode ret = this.nodeManager.getPublicNode(MongoDbConnector.stringToObjectId(nodeId));
        if (ret == null) {
            throw new ApiException(Status.NOT_FOUND, NodeApi.PUBLIC_NODE_NOT_FOUND_MESSAGE);
        }
        return ret;
    }

    @Override
    public List<PrivateNode> listPrivateNodes() {
        if (this.sessionUser.isAdmin()) {
            return this.nodeManager.getPrivateNodes();
        } else {
            return this.nodeManager.getPrivateNodesForUser(this.sessionUser);
        }
    }

    @Override
    public List<PublicNode> listPublicNodes() {
        return this.nodeManager.getPublicNodes();
    }

    @Override
    public List<UnidentifiedNode> listUnidentifiedNodes() throws AuthorizationException {
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
            throws AuthorizationException,
            NodePoolNotFoundException,
            InvalidObjectIdException {
        this.assertUserIsAdmin();
        final NodePool nodePool = this.getNodePool(nodePoolId);

        if (!nodePool.getId().equals(updatedNodePool.getId())) {
            throw new ApiException(Status.FORBIDDEN, "Cannot update the nodepool id");
        }

        return this.nodeManager.saveNodePool(updatedNodePool);
    }

    @Override
    public void deleteNodePool(final String nodePoolId) throws AuthorizationException,
            InvalidObjectIdException,
            NotFoundException {
        this.assertUserIsAdmin();
        final NodePool nodePool = this.getNodePool(nodePoolId);
        this.nodeManager.deleteNodePool(nodePool);
    }

    @Override
    public NodePool getNodePool(final String nodePoolId) throws AuthorizationException,
            InvalidObjectIdException,
            NodePoolNotFoundException {
        final ObjectId oid = MongoDbConnector.stringToObjectId(nodePoolId);
        final NodePool nodePool = this.nodeManager.getNodePool(oid);
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
