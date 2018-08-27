/**
 * File NodeRestApi.java
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

package org.flexiblepower.rest;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;

import org.bson.types.ObjectId;
import org.flexiblepower.api.NodeApi;
import org.flexiblepower.connectors.MongoDbConnector;
import org.flexiblepower.exceptions.ApiException;
import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.InvalidObjectIdException;
import org.flexiblepower.exceptions.NodePoolNotFoundException;
import org.flexiblepower.exceptions.NotFoundException;
import org.flexiblepower.model.Node;
import org.flexiblepower.model.NodePool;
import org.flexiblepower.model.PrivateNode;
import org.flexiblepower.model.PublicNode;
import org.flexiblepower.model.UnidentifiedNode;
import org.flexiblepower.model.User;
import org.flexiblepower.orchestrator.NodeManager;
import org.flexiblepower.orchestrator.UserManager;

import lombok.extern.slf4j.Slf4j;

/**
 * NodeRestApi
 *
 * @version 0.1
 * @since Mar 30, 2017
 */
@Slf4j
public class NodeRestApi extends BaseApi implements NodeApi {

    private static final Map<String, Comparator<UnidentifiedNode>> NODE_SORT_MAP = new HashMap<>();
    private static final Map<String, Comparator<PrivateNode>> PRIVATENODE_SORT_MAP = new HashMap<>();
    private static final Map<String, Comparator<PublicNode>> PUBLICNODE_SORT_MAP = new HashMap<>();
    static {
        NodeRestApi.NODE_SORT_MAP.put("default", Comparator.comparing((n) -> n.getId().toString()));
        NodeRestApi.NODE_SORT_MAP.put("id", Comparator.comparing((n) -> n.getId().toString()));
        NodeRestApi.NODE_SORT_MAP.put("name", Comparator.comparing(Node::getName));
        NodeRestApi.NODE_SORT_MAP.put("hostname", Comparator.comparing(Node::getHostname));
        NodeRestApi.NODE_SORT_MAP.put("status", Comparator.comparing(Node::getStatus));
        NodeRestApi.NODE_SORT_MAP.put("dockerId", Comparator.comparing(Node::getDockerId));
        NodeRestApi.NODE_SORT_MAP.put("architecture", Comparator.comparing(Node::getArchitecture));
        NodeRestApi.NODE_SORT_MAP.put("lastSync", Comparator.comparing(Node::getLastSync));

        NodeRestApi.PRIVATENODE_SORT_MAP.put("default", Comparator.comparing((n) -> n.getId().toString()));
        NodeRestApi.PRIVATENODE_SORT_MAP.put("id", Comparator.comparing((n) -> n.getId().toString()));
        NodeRestApi.PRIVATENODE_SORT_MAP.put("name", Comparator.comparing(Node::getName));
        NodeRestApi.PRIVATENODE_SORT_MAP.put("hostname", Comparator.comparing(Node::getHostname));
        NodeRestApi.PRIVATENODE_SORT_MAP.put("status", Comparator.comparing(Node::getStatus));
        NodeRestApi.PRIVATENODE_SORT_MAP.put("dockerId", Comparator.comparing(Node::getDockerId));
        NodeRestApi.PRIVATENODE_SORT_MAP.put("architecture",Comparator.comparing(Node::getArchitecture));
        NodeRestApi.PRIVATENODE_SORT_MAP.put("lastSync", Comparator.comparing(Node::getLastSync));
        NodeRestApi.PRIVATENODE_SORT_MAP.put("userId",Comparator.comparing((n) ->
                UserManager.getInstance().getUser(n.getUserId()).getUsername()));

        NodeRestApi.PUBLICNODE_SORT_MAP.put("default", Comparator.comparing((n) -> n.getId().toString()));
        NodeRestApi.PUBLICNODE_SORT_MAP.put("id", Comparator.comparing((n) -> n.getId().toString()));
        NodeRestApi.PUBLICNODE_SORT_MAP.put("name", Comparator.comparing(Node::getName));
        NodeRestApi.PUBLICNODE_SORT_MAP.put("hostname", Comparator.comparing(Node::getHostname));
        NodeRestApi.PUBLICNODE_SORT_MAP.put("status", Comparator.comparing(Node::getStatus));
        NodeRestApi.PUBLICNODE_SORT_MAP.put("dockerId", Comparator.comparing(Node::getDockerId));
        NodeRestApi.PUBLICNODE_SORT_MAP.put("architecture",Comparator.comparing(Node::getArchitecture));
        NodeRestApi.PUBLICNODE_SORT_MAP.put("lastSync", Comparator.comparing(Node::getLastSync));
        NodeRestApi.PUBLICNODE_SORT_MAP.put("nodePoolId", Comparator.comparing((n) ->
                NodeManager.getInstance().getNodePool(n.getNodePoolId()).getName()));
    }

    /**
     * Create the REST API with the headers from the HTTP request (will be injected by the HTTP server)
     *
     * @param httpHeaders The headers from the HTTP request for authorization
     */
    protected NodeRestApi(@Context final HttpHeaders httpHeaders) {
        super(httpHeaders);
    }

    @Override
    public PrivateNode createPrivateNode(final PrivateNode newNode) throws AuthorizationException,
            InvalidObjectIdException, NotFoundException {
        this.assertUserIsAdmin();
        // TODO this is a hack. The UI gives the id of the Unidentified node, not of the dockerId...
        final ObjectId oid = MongoDbConnector.stringToObjectId(newNode.getDockerId());
        final UnidentifiedNode un = NodeManager.getInstance().getUnidentifiedNode(oid);
        if (un == null) {
            throw new ApiException(Status.NOT_FOUND, NodeApi.UNIDENTIFIED_NODE_NOT_FOUND_MESSAGE);
        }

        final User owner = UserManager.getInstance().getUser(newNode.getUserId());
        if (owner == null) {
            throw new NodePoolNotFoundException();
        }

        if ((newNode.getName() != null) && !newNode.getName().isEmpty()) {
            un.setName(newNode.getName());
        } else {
            un.setName(un.getHostname());
        }

        NodeRestApi.log.info("Making node " + newNode.getDockerId() + " into a private node");
        return NodeManager.getInstance().makeUnidentifiedNodePrivate(un, owner);
    }

    @Override
    public PublicNode createPublicNode(final PublicNode newNode) throws AuthorizationException,
            InvalidObjectIdException, NodePoolNotFoundException {
        this.assertUserIsAdmin();
        // TODO this is a hack. The UI gives the id of the Unidentified node, not of the dockerId...
        final ObjectId oid = MongoDbConnector.stringToObjectId(newNode.getDockerId());
        final UnidentifiedNode un = NodeManager.getInstance().getUnidentifiedNode(oid);

        if (un == null) {
            throw new ApiException(Status.NOT_FOUND, NodeApi.UNIDENTIFIED_NODE_NOT_FOUND_MESSAGE);
        }

        final NodePool nodePool = NodeManager.getInstance().getNodePool(newNode.getNodePoolId());
        if (nodePool == null) {
            throw new NodePoolNotFoundException();
        }

        if ((newNode.getName() != null) && !newNode.getName().isEmpty()) {
            un.setName(newNode.getName());
        } else {
            un.setName(un.getHostname());
        }

        NodeRestApi.log.info("Making node " + newNode.getDockerId() + " into a public node");
        return NodeManager.getInstance().makeUnidentifiedNodePublic(un, nodePool);
    }

    @Override
    public PrivateNode updatePrivateNode(final String nodeId, final PrivateNode updatedPrivateNode)
            throws AuthorizationException,
            NotFoundException,
            InvalidObjectIdException {
        final PrivateNode node = this.getPrivateNode(nodeId);

        if ((node.getName() == null) || !node.getName().equals(updatedPrivateNode.getName())) {
            node.setName(updatedPrivateNode.getName());
        }

        MongoDbConnector.getInstance().save(node);
        return node;
    }

    @Override
    public PublicNode updatePublicNode(final String nodeId, final PublicNode updatedPublicNode)
            throws AuthorizationException,
            NotFoundException,
            InvalidObjectIdException {
        final PublicNode node = this.getPublicNode(nodeId);

        if ((node.getName() == null) || !node.getName().equals(updatedPublicNode.getName())) {
            node.setName(updatedPublicNode.getName());
        }

        MongoDbConnector.getInstance().save(node);
        return node;
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
    public List<PrivateNode> listPrivateNodes(final int page,
            final int perPage,
            final String sortDir,
            final String sortField,
            final String filters) throws AuthorizationException {
        // TODO implement pagination
        final NodeManager nm = NodeManager.getInstance();

        List<PrivateNode> content;
        if (this.sessionUser == null) {
            throw new AuthorizationException();
        } else if (this.sessionUser.isAdmin()) {
            content = nm.getPrivateNodes();
        } else {
            content = nm.getPrivateNodesForUser(this.sessionUser);
        }

        // Do some filtering
        final Map<String, Object> filter = MongoDbConnector.parseFilters(filters);
        RestUtils.filterMultiContent(content, PrivateNode::getId, filter, "ids[]");
        RestUtils.filterContent(content, PrivateNode::getName, filter, "name");

        this.addTotalCount(content.size());
        RestUtils.orderContent(content, NodeRestApi.PRIVATENODE_SORT_MAP, sortField, sortDir);

        this.addTotalCount(content.size());
        return RestUtils.paginate(content, page, perPage);
    }

    @Override
    public List<PublicNode> listPublicNodes(final int page,
            final int perPage,
            final String sortDir,
            final String sortField,
            final String filters) throws AuthorizationException {
        this.assertUserIsLoggedIn();

        final List<PublicNode> content = NodeManager.getInstance().getPublicNodes();

        // Do some filtering
        final Map<String, Object> filter = MongoDbConnector.parseFilters(filters);
        RestUtils.filterMultiContent(content, PublicNode::getId, filter, "ids[]");
        RestUtils.filterContent(content, PublicNode::getName, filter, "name");

        RestUtils.orderContent(content, NodeRestApi.PUBLICNODE_SORT_MAP, sortField, sortDir);

        this.addTotalCount(content.size());
        return RestUtils.paginate(content, page, perPage);
    }

    @Override
    public List<UnidentifiedNode> listUnidentifiedNodes(final int page,
            final int perPage,
            final String sortDir,
            final String sortField,
            final String filters) throws AuthorizationException {
        this.assertUserIsAdmin();

        final List<UnidentifiedNode> content = NodeManager.getInstance().getUnidentifiedNodes();
        RestUtils.orderContent(content, NodeRestApi.NODE_SORT_MAP, sortField, sortDir);

        this.addTotalCount(content.size());
        return RestUtils.paginate(content, page, perPage);
    }

    @Override
    public NodePool createNodePool(final NodePool newNodePool) throws AuthorizationException {
        this.assertUserIsAdmin();
        return NodeManager.getInstance().saveNodePool(newNodePool);
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

        return NodeManager.getInstance().saveNodePool(updatedNodePool);
    }

    @Override
    public void deleteNodePool(final String nodePoolId) throws AuthorizationException,
            InvalidObjectIdException,
            NodePoolNotFoundException {
        this.assertUserIsAdmin();
        final NodePool nodePool = this.getNodePool(nodePoolId);
        NodeManager.getInstance().deleteNodePool(nodePool);
    }

    @Override
    public NodePool getNodePool(final String nodePoolId) throws AuthorizationException,
            InvalidObjectIdException,
            NodePoolNotFoundException {
        this.assertUserIsLoggedIn();
        final ObjectId oid = MongoDbConnector.stringToObjectId(nodePoolId);
        final NodePool nodePool = NodeManager.getInstance().getNodePool(oid);
        if (nodePool == null) {
            throw new NodePoolNotFoundException();
        }
        return nodePool;
    }

    @Override
    public List<NodePool> listNodePools(final int page,
            final int perPage,
            final String sortDir,
            final String sortField,
            final String filters) throws AuthorizationException {
        this.assertUserIsLoggedIn();
        final Map<String, Object> filter = MongoDbConnector.parseFilters(filters);
        final List<NodePool> content;

        if (filter.containsKey("ids[]")) {
            content = new LinkedList<>();

            @SuppressWarnings("unchecked")
            final List<String> ids = (List<String>) filter.get("ids[]");
            for (final String id : ids) {
                try {
                    content.add(NodeManager.getInstance().getNodePool(MongoDbConnector.stringToObjectId(id)));
                } catch (final InvalidObjectIdException e) {
                    NodeRestApi.log.debug("Invalid objectId in list: {}", id);
                }
            }
        } else {
            content = NodeManager.getInstance().listNodePools(page, perPage, sortDir, sortField, filter);
        }
        this.addTotalCount(NodeManager.getInstance().countNodePools(filter));
        return content;
    }

}
