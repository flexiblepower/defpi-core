package org.flexiblepower.rest;

import java.util.Collection;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.flexiblepower.orchestrator.DockerConnector;
import org.flexiblepower.orchestrator.MongoDbConnector;

import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.swarm.Node;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("node")
public class NodeApi {

    private final MongoDbConnector db = new MongoDbConnector();

    private void initUser(final HttpHeaders httpHeaders) {
        final String username = httpHeaders.getHeaderString("username");
        final String password = httpHeaders.getHeaderString("password");
        this.db.setApplicationUser(this.db.getUser(username, password));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Node> listNodes(@Context final HttpHeaders httpHeaders) {
        // this.initUser(httpHeaders);
        try {
            final List<Node> list = DockerConnector.init().listNodes();
            return list;
        } catch (DockerException | InterruptedException | DockerCertificateException e) {
            throw new ApiException(e);
        }
    }

    // @GET
    // @Produces(MediaType.APPLICATION_JSON)
    // @Path("new")
    // public Response newHost(@javax.ws.rs.core.Context final HttpHeaders httpHeaders) {
    // if (this.initUser(httpHeaders)) {
    // final String command = Swarm.newHost();
    // final String publicCommand = command.replace("sudo docker run",
    // "sudo docker run -e CATTLE_HOST_LABELS='type=public'");
    // final String privateCommand = command.replace("sudo docker run",
    // "sudo docker run -e CATTLE_HOST_LABELS='type=private&user=" + this.userId + "'");
    // ;
    // return Response
    // .ok("{\"publicCommand\":\"" + publicCommand + "\", \"privateCommand\": \"" + privateCommand + "\"}")
    // .build();
    // }
    // return Response.status(Status.UNAUTHORIZED).build();
    // }
    //
    // @POST
    // @Path("{action}/{host}")
    // public Response hostAction(@javax.ws.rs.core.Context final HttpHeaders httpHeaders,
    // @PathParam("action") final String action,
    // @PathParam("host") final String host) throws DockerCertificateException,
    // DockerException,
    // InterruptedException {
    // HostsRest.logger.info("Host action: " + action + ", " + host);
    // if (this.initUser(httpHeaders)) {
    // final int status = Swarm.hostAction(host, action);
    // HostsRest.logger.info("Rancher status: " + status);
    // Swarm.syncHosts();
    // return Response.status(status).build();
    // }
    // return Response.status(Status.UNAUTHORIZED).build();
    // }
}
