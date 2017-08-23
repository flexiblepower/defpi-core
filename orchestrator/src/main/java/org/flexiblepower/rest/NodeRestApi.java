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
import org.flexiblepower.connectors.MongoDbConnector;
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
import org.flexiblepower.orchestrator.NodeManager;
import org.flexiblepower.orchestrator.UserManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NodeRestApi extends BaseApi implements NodeApi {

    protected NodeRestApi(@Context final HttpHeaders httpHeaders) {
        super(httpHeaders);
    }

    @Override
    public PrivateNode createPrivateNode(final PrivateNode newNode)
            throws AuthorizationException, InvalidObjectIdException {
        this.assertUserIsAdmin();
        // TODO this is a hack. The UI gives the id of the Unidentified node, not of the dockerId...
        final ObjectId oid = MongoDbConnector.stringToObjectId(newNode.getDockerId());
        final UnidentifiedNode un = NodeManager.getInstance().getUnidentifiedNode(oid);
        if (un == null) {
            throw new ApiException(Status.NOT_FOUND, NodeApi.UNIDENTIFIED_NODE_NOT_FOUND_MESSAGE);
        }

        final User owner = UserManager.getInstance().getUser(newNode.getUserId());
        if (owner == null) {
            throw new ApiException(Status.NOT_FOUND, UserApi.USER_NOT_FOUND_MESSAGE);
        }

        NodeRestApi.log.info("Making node " + newNode.getDockerId() + " into a private node");
        return NodeManager.getInstance().makeUnidentifiedNodePrivate(un, owner);
    }

    @Override
    public PublicNode createPublicNode(final PublicNode newNode)
            throws AuthorizationException, InvalidObjectIdException {
        this.assertUserIsAdmin();
        // TODO this is a hack. The UI gives the id of the Unidentified node, not of the dockerId...
        final ObjectId oid = MongoDbConnector.stringToObjectId(newNode.getDockerId());
        final UnidentifiedNode un = NodeManager.getInstance().getUnidentifiedNode(oid);

        if (un == null) {
            throw new ApiException(Status.NOT_FOUND, NodeApi.UNIDENTIFIED_NODE_NOT_FOUND_MESSAGE);
        }

        final NodePool nodePool = NodeManager.getInstance().getNodePool(newNode.getNodePoolId());
        if (nodePool == null) {
            throw new ApiException(Status.NOT_FOUND, NodeApi.NODE_POOL_NOT_FOUND_MESSAGE);
        }

        NodeRestApi.log.info("Making node " + newNode.getDockerId() + " into a public node");
        return NodeManager.getInstance().makeUnidentifiedNodePublic(un, nodePool);
    }

    @Override
    public void deletePrivateNode(final String nodeId) throws AuthorizationException, InvalidObjectIdException {
        this.assertUserIsAdmin();

        final ObjectId oid = MongoDbConnector.stringToObjectId(nodeId);
        final PrivateNode privateNode = NodeManager.getInstance().getPrivateNode(oid);
        if (privateNode == null) {
            throw new ApiException(Status.NOT_FOUND, NodeApi.PRIVATE_NODE_NOT_FOUND_MESSAGE);
        }
        NodeManager.getInstance().deletePrivateNode(privateNode);
    }

    @Override
    public void deletePublicNode(final String nodeId) throws AuthorizationException, InvalidObjectIdException {
        this.assertUserIsAdmin();

        final ObjectId oid = MongoDbConnector.stringToObjectId(nodeId);
        final PublicNode publicNode = NodeManager.getInstance().getPublicNode(oid);

        if (publicNode == null) {
            throw new ApiException(Status.NOT_FOUND, NodeApi.PUBLIC_NODE_NOT_FOUND_MESSAGE);
        }
        NodeManager.getInstance().deletePublicNode(publicNode);
    }

    @Override
    public PrivateNode getPrivateNode(final String id) throws AuthorizationException, InvalidObjectIdException {
        final ObjectId nodeId = MongoDbConnector.stringToObjectId(id);
        final PrivateNode ret = NodeManager.getInstance().getPrivateNode(nodeId);

        if (ret == null) {
            throw new ApiException(Status.NOT_FOUND, NodeApi.PRIVATE_NODE_NOT_FOUND_MESSAGE);
        }

        this.assertUserIsAdminOrEquals(ret.getUserId());
        return ret;
    }

    @Override
    public PublicNode getPublicNode(final String nodeId) throws InvalidObjectIdException, AuthorizationException {
        this.assertUserIsLoggedIn();
        final PublicNode ret = NodeManager.getInstance().getPublicNode(MongoDbConnector.stringToObjectId(nodeId));
        if (ret == null) {
            throw new ApiException(Status.NOT_FOUND, NodeApi.PUBLIC_NODE_NOT_FOUND_MESSAGE);
        }
        return ret;
    }

    @Override
    public List<PrivateNode> listPrivateNodes() throws AuthorizationException {
        if (this.sessionUser == null) {
            throw new AuthorizationException();
        } else if (this.sessionUser.isAdmin()) {
            return NodeManager.getInstance().getPrivateNodes();
        } else {
            return NodeManager.getInstance().getPrivateNodesForUser(this.sessionUser);
        }
    }

    @Override
    public List<PublicNode> listPublicNodes() throws AuthorizationException {
        this.assertUserIsLoggedIn();
        return NodeManager.getInstance().getPublicNodes();
    }

    @Override
    public List<UnidentifiedNode> listUnidentifiedNodes() throws AuthorizationException {
        this.assertUserIsAdmin();
        return NodeManager.getInstance().getUnidentifiedNodes();
    }

    @Override
    public NodePool createNodePool(final NodePool newNodePool) throws AuthorizationException {
        this.assertUserIsAdmin();
        return NodeManager.getInstance().saveNodePool(newNodePool);
    }

    @Override
    public NodePool updateNodePool(final String nodePoolId, final NodePool updatedNodePool)
            throws AuthorizationException, NodePoolNotFoundException, InvalidObjectIdException {
        this.assertUserIsAdmin();
        final NodePool nodePool = this.getNodePool(nodePoolId);

        if (!nodePool.getId().equals(updatedNodePool.getId())) {
            throw new ApiException(Status.FORBIDDEN, "Cannot update the nodepool id");
        }

        return NodeManager.getInstance().saveNodePool(updatedNodePool);
    }

    @Override
    public void deleteNodePool(final String nodePoolId)
            throws AuthorizationException, InvalidObjectIdException, NotFoundException {
        this.assertUserIsAdmin();
        final NodePool nodePool = this.getNodePool(nodePoolId);
        NodeManager.getInstance().deleteNodePool(nodePool);
    }

    @Override
    public NodePool getNodePool(final String nodePoolId)
            throws AuthorizationException, InvalidObjectIdException, NodePoolNotFoundException {
        this.assertUserIsLoggedIn();
        final ObjectId oid = MongoDbConnector.stringToObjectId(nodePoolId);
        final NodePool nodePool = NodeManager.getInstance().getNodePool(oid);
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
        this.assertUserIsLoggedIn();
        final Map<String, Object> filter = MongoDbConnector.parseFilters(filters);
        return Response.status(Status.OK.getStatusCode())
                .header("X-Total-Count", Integer.toString(NodeManager.getInstance().countNodePools(filter)))
                .entity(NodeManager.getInstance().listNodePools(page, perPage, sortDir, sortField, filter))
                .build();
    }
}
