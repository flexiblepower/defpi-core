package org.flexiblepower.rest;

import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;

import org.flexiblepower.api.NodeApi;
import org.flexiblepower.exceptions.ApiException;
import org.flexiblepower.exceptions.InvalidObjectIdException;
import org.flexiblepower.model.PrivateNode;
import org.flexiblepower.model.PublicNode;
import org.flexiblepower.model.UnidentifiedNode;
import org.flexiblepower.orchestrator.DockerConnector;

import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.swarm.Node;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NodeRestApi extends BaseApi implements NodeApi {

    protected NodeRestApi(@Context final HttpHeaders httpHeaders, @Context final SecurityContext securityContext) {
        super(httpHeaders, securityContext);
    }

    @Override
    public String createPrivateNode(final PrivateNode newNode) {
        return this.db.insertNode(newNode);
    }

    @Override
    public String createPublicNode(final PublicNode newNode) {
        return this.db.insertNode(newNode);
    }

    @Override
    public void deletePrivateNode(final String nodeId) throws InvalidObjectIdException {
        this.db.deleteNode(nodeId);
    }

    @Override
    public void deletePublicNode(final String nodeId) throws InvalidObjectIdException {
        this.db.deleteNode(nodeId);
    }

    @Override
    public PrivateNode getPrivateNode(final String nodeId) throws InvalidObjectIdException {
        return this.db.getPrivateNode(nodeId);
    }

    @Override
    public PublicNode getPublicNode(final String nodeId) throws InvalidObjectIdException {
        return this.db.getPublicNode(nodeId);
    }

    @Override
    public List<PrivateNode> listPrivateNodes() {
        return this.db.getPrivateNodes();
    }

    @Override
    public List<PublicNode> listPublicNodes() {
        return this.db.getPublicNodes();
    }

    @Override
    public List<UnidentifiedNode> listUnidentifiedNodes() {
        try {
            final List<Node> nodeList = DockerConnector.init().listNodes();
            final LinkedList<UnidentifiedNode> ret = new LinkedList<>();
            for (final Node node : nodeList) {
                // TODO
                // if ((this.db.getPublicNode(node.id()) == null) && (this.db.getPrivateNode(node.id()) == null)) {
                ret.add(new UnidentifiedNode(node.description().hostname()));
                // }
            }
            return ret;
        } catch (DockerException | InterruptedException | DockerCertificateException e) {
            e.printStackTrace();
            throw new ApiException(e);
        }

    }
}
