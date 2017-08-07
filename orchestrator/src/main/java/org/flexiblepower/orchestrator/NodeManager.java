/**
 * File NodeManager.java
 *
 * Copyright 2017 TNO
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
 * @author wilco
 * @version 0.1
 * @since May 1, 2017
 */
public class NodeManager {

    private static final long ALLOWED_CACHE_TIME_MS = 60000;
    private static NodeManager instance = null;

    public synchronized static NodeManager getInstance() {
        if (NodeManager.instance == null) {
            NodeManager.instance = new NodeManager();
        }
        return NodeManager.instance;
    }

    private final MongoDbConnector db = MongoDbConnector.getInstance();
    private final DockerConnector docker = DockerConnector.getInstance();

    private NodeManager() {

    }

    private void syncAllNodes() {
        // build map with all nodecs
        final Map<String, Node> dockerNodes = new HashMap<>();
        for (final Node n : this.docker.listNodes()) {
            dockerNodes.put(n.id(), n);
        }

        // Go through private nodes
        for (final PrivateNode pn : this.db.list(PrivateNode.class)) {
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
            this.db.save(pn);
        }

        // Go through public nodes
        for (final PublicNode pn : this.db.list(PublicNode.class)) {
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
            this.db.save(pn);
        }

        // Go through unidentified nodes
        for (final UnidentifiedNode un : this.db.list(UnidentifiedNode.class)) {
            final Node node = dockerNodes.get(un.getDockerId());
            if (node == null) {
                // Node was unidentified, now it's gone. Remove from db.
                this.db.delete(un);
            } else {
                un.setStatus(DockerNodeStatus.fromString(node.status().state()));
                un.setHostname(node.description().hostname());
                un.setLastSync(new Date());
                dockerNodes.remove(un.getDockerId());
                this.db.save(un);
            }
        }

        // All nodes that are left are new Unidentified Nodes
        for (final Node n : dockerNodes.values()) {
            final UnidentifiedNode unidentifiedNode = new UnidentifiedNode(n.id(),
                    n.description().hostname(),
                    Architecture.fromString(n.description().platform().architecture()));
            this.db.save(unidentifiedNode);
        }
    }

    private <T extends org.flexiblepower.model.Node> List<T> getNodesAndSync(final Class<T> nodeType) {
        final List<T> nodes = this.db.list(nodeType);
        final Date now = new Date();
        for (final T node : nodes) {
            if ((node.getLastSync().getTime() + NodeManager.ALLOWED_CACHE_TIME_MS) < now.getTime()) {
                // Data too old, resync first
                this.syncAllNodes();
                // Retrieve new data
                return this.db.list(nodeType);
            }
        }
        return nodes;
    }

    private <T extends org.flexiblepower.model.Node> T getNodeAndSync(final Class<T> nodeType, final ObjectId id) {
        final T node = this.db.get(nodeType, id);
        if ((node.getLastSync().getTime() + NodeManager.ALLOWED_CACHE_TIME_MS) < System.currentTimeMillis()) {
            // Data too old, resync first
            this.syncAllNodes();
            // Retrieve new data
            return this.db.get(nodeType, id);
        }
        return node;
    }

    public List<UnidentifiedNode> getUnidentifiedNodes() {
        // Always sync
        this.syncAllNodes();
        return this.db.list(UnidentifiedNode.class);
    }

    public UnidentifiedNode getUnidentifiedNode(final ObjectId id) {
        return this.getNodeAndSync(UnidentifiedNode.class, id);
    }

    public UnidentifiedNode getUnidentifiedNodeByDockerId(final String dockerId) {
        this.syncAllNodes();
        return this.db.getUnidentifiedNodeByDockerId(dockerId);
    }

    public List<PublicNode> getPublicNodesInNodePool(final NodePool nodePool) {
        final List<PublicNode> result = new ArrayList<>();
        for (final PublicNode pn : this.getPublicNodes()) {
            if (nodePool.getId().equals(pn.getNodePoolId())) {
                result.add(pn);
            }
        }
        return result;
    }

    public List<PublicNode> getPublicNodes() {
        return this.getNodesAndSync(PublicNode.class);
    }

    public PublicNode getPublicNode(final ObjectId id) {
        return this.getNodeAndSync(PublicNode.class, id);
    }

    public List<PrivateNode> getPrivateNodes() {
        return this.getNodesAndSync(PrivateNode.class);
    }

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

    public PrivateNode getPrivateNode(final ObjectId id) {
        return this.getNodeAndSync(PrivateNode.class, id);
    }

    public List<NodePool> getNodePools() {
        return this.db.list(NodePool.class);
    }

    public NodePool getNodePool(final ObjectId id) {
        return this.db.get(NodePool.class, id);
    }

    public PrivateNode makeUnidentifiedNodePrivate(final UnidentifiedNode unidentifiedNode, final User owner) {
        final PrivateNode privateNode = new PrivateNode(unidentifiedNode, owner);
        this.db.save(privateNode);
        this.db.delete(unidentifiedNode);
        return privateNode;
    }

    public PublicNode makeUnidentifiedNodePublic(final UnidentifiedNode unidentifiedNode, final NodePool nodePool) {
        final PublicNode publicNode = new PublicNode(unidentifiedNode, nodePool);
        this.db.save(publicNode);
        this.db.delete(unidentifiedNode);
        return publicNode;
    }

    public void deletePublicNode(final PublicNode publicNode) {
        this.db.delete(publicNode);
        this.syncAllNodes();
    }

    public void deletePrivateNode(final PrivateNode privateNode) {
        this.db.delete(privateNode);
        this.syncAllNodes();
    }

    public NodePool saveNodePool(final NodePool newNodePool) {
        if ((newNodePool.getName() == null) || newNodePool.getName().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        this.db.save(newNodePool);
        return newNodePool;
    }

    public void deleteNodePool(final NodePool nodePool) {
        this.db.delete(nodePool);
    }

    public List<NodePool> listNodePools(final int page,
            final int perPage,
            final String sortDir,
            final String sortField,
            final Map<String, Object> filter) {
        return this.db.list(NodePool.class, page, perPage, sortDir, sortField, filter);
    }

    public int countNodePools(final Map<String, Object> filter) {
        return this.db.totalCount(NodePool.class, filter);
    }

}
