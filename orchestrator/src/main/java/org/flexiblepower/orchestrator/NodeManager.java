/**
 * File NodeManager.java
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
package org.flexiblepower.orchestrator;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.flexiblepower.connectors.DockerConnector;
import org.flexiblepower.connectors.MongoDbConnector;
import org.flexiblepower.model.Architecture;
import org.flexiblepower.model.Node.DockerNodeStatus;
import org.flexiblepower.model.NodePool;
import org.flexiblepower.model.PrivateNode;
import org.flexiblepower.model.PublicNode;
import org.flexiblepower.model.UnidentifiedNode;
import org.flexiblepower.model.User;

import com.spotify.docker.client.messages.swarm.Node;

/**
 * NodeManager
 *
 * @version 0.1
 * @since May 1, 2017
 */
@SuppressWarnings("static-method")
public class NodeManager {

    private static final long ALLOWED_CACHE_TIME_MS = 60000;
    private static NodeManager instance = null;

    /**
     * @return The singleton instance of the NodeManager
     */
    public static NodeManager getInstance() {
        if (NodeManager.instance == null) {
            NodeManager.instance = new NodeManager();
        }
        return NodeManager.instance;
    }

    /*
     * Empty private contructor for singleton design pattern
     */
    private NodeManager() {
    }

    private void syncAllNodes() {
        final MongoDbConnector mongo = MongoDbConnector.getInstance();
        final DockerConnector docker = DockerConnector.getInstance();

        // build map with all nodecs
        final Map<String, Node> dockerNodes = new HashMap<>();
        for (final Node n : docker.listNodes()) {
            dockerNodes.put(n.id(), n);
        }

        // Go through private nodes
        for (final PrivateNode pn : mongo.list(PrivateNode.class)) {
            final Node node = dockerNodes.get(pn.getDockerId());
            if (node == null) {
                pn.setStatus(DockerNodeStatus.MISSING);
            } else {
                pn.setStatus(DockerNodeStatus.fromString(node.status().state()));
                pn.setHostname(node.description().hostname());
                pn.setArchitecture(Architecture.fromString(node.description().platform().architecture()));
            }
            pn.setLastSync(new Date());
            dockerNodes.remove(pn.getDockerId());
            mongo.save(pn);
        }

        // Go through public nodes
        for (final PublicNode pn : mongo.list(PublicNode.class)) {
            final Node node = dockerNodes.get(pn.getDockerId());
            if (node == null) {
                pn.setStatus(DockerNodeStatus.MISSING);
            } else {
                pn.setStatus(DockerNodeStatus.fromString(node.status().state()));
                pn.setHostname(node.description().hostname());
                pn.setArchitecture(Architecture.fromString(node.description().platform().architecture()));
            }
            pn.setLastSync(new Date());
            dockerNodes.remove(pn.getDockerId());
            mongo.save(pn);
        }

        // Go through unidentified nodes
        for (final UnidentifiedNode un : mongo.list(UnidentifiedNode.class)) {
            final Node node = dockerNodes.get(un.getDockerId());
            if (node == null) {
                // Node was unidentified, now it's gone. Remove from db.
                mongo.delete(un);
            } else {
                un.setStatus(DockerNodeStatus.fromString(node.status().state()));
                un.setHostname(node.description().hostname());
                un.setLastSync(new Date());
                dockerNodes.remove(un.getDockerId());
                mongo.save(un);
            }
        }

        // All nodes that are left are new Unidentified Nodes
        for (final Node n : dockerNodes.values()) {
            final UnidentifiedNode unidentifiedNode = new UnidentifiedNode(n.id(),
                    n.description().hostname(),
                    Architecture.fromString(n.description().platform().architecture()));
            mongo.save(unidentifiedNode);
        }
    }

    private <T extends org.flexiblepower.model.Node> List<T> getNodesAndSync(final Class<T> nodeType) {
        final MongoDbConnector mongo = MongoDbConnector.getInstance();

        final List<T> nodes = mongo.list(nodeType);
        final Date now = new Date();
        for (final T node : nodes) {
            if ((node.getLastSync().getTime() + NodeManager.ALLOWED_CACHE_TIME_MS) < now.getTime()) {
                // Data too old, resync first
                this.syncAllNodes();
                // Retrieve new data
                return mongo.list(nodeType);
            }
        }
        return nodes;
    }

    private <T extends org.flexiblepower.model.Node> T getNodeAndSync(final Class<T> nodeType, final ObjectId id) {
        final MongoDbConnector mongo = MongoDbConnector.getInstance();

        final T node = mongo.get(nodeType, id);
        if ((node != null)
                && ((node.getLastSync().getTime() + NodeManager.ALLOWED_CACHE_TIME_MS) < System.currentTimeMillis())) {
            // Data too old, resync first
            this.syncAllNodes();
            // Retrieve new data
            return mongo.get(nodeType, id);
        }
        return node;
    }

    /**
     * @return The list of {@link UnidentifiedNode}s in the database.
     */
    public List<UnidentifiedNode> getUnidentifiedNodes() {
        // Always sync
        this.syncAllNodes();
        return MongoDbConnector.getInstance().list(UnidentifiedNode.class);
    }

    /**
     * Get the {@link UnidentifiedNode} from the database with the provided ObjectId, or {@code null} if no such node
     * exists.
     *
     * @param id The ObjectId to find in the database
     * @return The node with the given id
     */
    public UnidentifiedNode getUnidentifiedNode(final ObjectId id) {
        return this.getNodeAndSync(UnidentifiedNode.class, id);
    }

    /**
     * Get the {@link UnidentifiedNode} that has the provided docker Id, or {@code null} if no such node exists.
     *
     * @param dockerId The docker generated id to find the node by
     * @return The node with the given docker id
     */
    public UnidentifiedNode getUnidentifiedNodeByDockerId(final String dockerId) {
        this.syncAllNodes();
        return MongoDbConnector.getInstance().getUnidentifiedNodeByDockerId(dockerId);
    }

    // public PublicNode getPublicNodeByDockerId(final String dockerId) {
    // this.syncAllNodes();
    // return MongoDbConnector.getInstance().getPublicNodeByDockerId(dockerId);
    // }

    /**
     * Get the list of all {@link PublicNode}s that are part of the provided node pool. If no nodes are registered in
     * the node pool, the empty list will be returned.
     *
     * @param nodePool The node pool to get the nodes from.
     * @return A List of all nodes that are in the node pool
     */
    public List<PublicNode> getPublicNodesInNodePool(final NodePool nodePool) {
        final List<PublicNode> result = new ArrayList<>();
        for (final PublicNode pn : this.getPublicNodes()) {
            if (nodePool.getId().equals(pn.getNodePoolId())) {
                result.add(pn);
            }
        }
        return result;
    }

    /**
     * @return The list of {@link PublicNode}s in the database.
     */
    public List<PublicNode> getPublicNodes() {
        return this.getNodesAndSync(PublicNode.class);
    }

    /**
     * Get the {@link PublicNode} from the database with the provided ObjectId, or {@code null} if no such node exists.
     *
     * @param id The ObjectId to find in the database
     * @return The node with the given id
     */
    public PublicNode getPublicNode(final ObjectId id) {
        return this.getNodeAndSync(PublicNode.class, id);
    }

    /**
     * @return The list of {@link PrivateNode}s in the database.
     */
    public List<PrivateNode> getPrivateNodes() {
        return this.getNodesAndSync(PrivateNode.class);
    }

    /**
     * Get the list of all {@link PrivateNode}s that are assigned to the provided user. If no nodes are owned by the
     * user, the empty list will be returned.
     *
     * @param owner The User who owns the list of nodes
     * @return A List of all nodes that are owner by this user
     */
    public List<PrivateNode> getPrivateNodesForUser(final User owner) {
        final List<PrivateNode> allNodes = this.getPrivateNodes();
        final List<PrivateNode> ret = new ArrayList<>();
        for (final PrivateNode node : allNodes) {
            if (node.getUserId().equals(owner.getId())) {
                ret.add(node);
            }
        }
        return ret;
    }

    /**
     * Get the {@link PrivateNode} from the database with the provided ObjectId, or {@code null} if no such node exists.
     *
     * @param id The ObjectId to find in the database
     * @return The node with the given id
     */
    public PrivateNode getPrivateNode(final ObjectId id) {
        return this.getNodeAndSync(PrivateNode.class, id);
    }

    /**
     * Find a node based on its' machine's hostname
     *
     * @param hostname The hostname of the node to find
     * @return The node that represents the machine with the given hostname
     */
    public org.flexiblepower.model.Node getNodeByHostname(final String hostname) {
        // Most likely it is a public node, so start here
        final List<PublicNode> publicNodes = this.getNodesAndSync(PublicNode.class);
        for (final PublicNode node : publicNodes) {
            if (node.getHostname().equals(hostname)) {
                return node;
            }
        }

        final List<PrivateNode> privateNodes = this.getNodesAndSync(PrivateNode.class);
        for (final PrivateNode node : privateNodes) {
            if (node.getHostname().equals(hostname)) {
                return node;
            }
        }
        return null;
    }

    /**
     * @return The list of {@link NodePool}s in the database.
     */
    public List<NodePool> getNodePools() {
        return MongoDbConnector.getInstance().list(NodePool.class);
    }

    /**
     * Get the {@link NodePool} from the database with the provided ObjectId, or {@code null} if no such node exists.
     *
     * @param id The ObjectId to find in the database
     * @return The node pool with the given id
     */
    public NodePool getNodePool(final ObjectId id) {
        return MongoDbConnector.getInstance().get(NodePool.class, id);
    }

    /**
     * Promote the provided {@link UnidentifiedNode} to private, by assigning it an owning user. This makes the node
     * eligible for running processes, but only processes that are owned by that user.
     *
     * @param unidentifiedNode The node to promote to a private node
     * @param owner The User to assign the node to
     * @return The private node that is the result of the promotion
     */
    public PrivateNode makeUnidentifiedNodePrivate(final UnidentifiedNode unidentifiedNode, final User owner) {
        final PrivateNode privateNode = new PrivateNode(unidentifiedNode, owner);
        MongoDbConnector.getInstance().save(privateNode);
        MongoDbConnector.getInstance().delete(unidentifiedNode);
        return privateNode;
    }

    /**
     * Promote the provided {@link UnidentifiedNode} to public, by assigning it a node pool. This makes the node
     * eligible for running processes from any user.
     *
     * @param unidentifiedNode The node to promote to a public node
     * @param nodePool The NodePool to assign the node to
     * @return The public node that is the result of the promotion
     */
    public PublicNode makeUnidentifiedNodePublic(final UnidentifiedNode unidentifiedNode, final NodePool nodePool) {
        final PublicNode publicNode = new PublicNode(unidentifiedNode, nodePool);
        MongoDbConnector.getInstance().save(publicNode);
        MongoDbConnector.getInstance().delete(unidentifiedNode);
        return publicNode;
    }

    /**
     * Delete the public node from the database, effectively demoting it to a {@link UnidentifiedNode}. This means the
     * node will no longer accept processes to run on it. Running processes are not removed by the NodeManager.
     *
     * @param publicNode The public node to demote
     */
    public void deletePublicNode(final PublicNode publicNode) {
        MongoDbConnector.getInstance().delete(publicNode);
        this.syncAllNodes();
    }

    /**
     * Delete the private node from the database, effectively demoting it to a {@link UnidentifiedNode}. This means the
     * node will no longer accept processes to run on it. Running processes are not removed by the NodeManager.
     *
     * @param privateNode The public node to demote
     */
    public void deletePrivateNode(final PrivateNode privateNode) {
        MongoDbConnector.getInstance().delete(privateNode);
        this.syncAllNodes();
    }

    /**
     * Save the node pool to the database, either because it is a new pool, or the updated version of an existing node
     * pool. If the name of the nodepool is empty or {@code null}, an exception will be thrown.
     *
     * @param newNodePool The new or updated node pool to save.
     * @return The nodepool as it is now stored in the database (with an ObjectId)
     * @throws IllegalArgumentException When the name of the node pool is null or empty.
     */
    public NodePool saveNodePool(final NodePool newNodePool) {
        if ((newNodePool.getName() == null) || newNodePool.getName().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        MongoDbConnector.getInstance().save(newNodePool);
        return newNodePool;
    }

    /**
     * Delete the node pool from the database. This means the node pool will no longer accept public node to be added to
     * it. Existing public nodes will not be removed by the node manager.
     *
     * @param nodePool The node pool to delete
     */
    public void deleteNodePool(final NodePool nodePool) {
        MongoDbConnector.getInstance().delete(nodePool);
    }

    /**
     * List node pools. It is possible to paginate, sort and filter all node pools depending on the provided arguments.
     *
     * @param page The page to view
     * @param perPage The amount of node pools to view per page, and thus the maximum amount of node pools returned
     * @param sortDir The direction to sort
     * @param sortField The field to sort on
     * @param filter A key/value map of filters
     * @return A list all node pools that match the filters, or a paginated subset thereof
     */
    public List<NodePool> listNodePools(final int page,
            final int perPage,
            final String sortDir,
            final String sortField,
            final Map<String, Object> filter) {
        return MongoDbConnector.getInstance().list(NodePool.class, page, perPage, sortDir, sortField, filter);
    }

    /**
     * Count all node pools currently stored in the database
     *
     * @param filter A filter to count a specific filtered subset of node pools, may be empty
     * @return The number of node pools that match the filter
     */
    public int countNodePools(final Map<String, Object> filter) {
        return MongoDbConnector.getInstance().totalCount(NodePool.class, filter);
    }

}
