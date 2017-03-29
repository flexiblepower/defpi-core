package org.flexiblepower.rest;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.bson.types.ObjectId;
import org.flexiblepower.exceptions.ApiException;
import org.flexiblepower.orchestrator.Database;
import org.flexiblepower.orchestrator.Hosts;
import org.flexiblepower.orchestrator.Swarm;
import org.flexiblepower.orchestrator.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.swarm.Node;

@Path("hosts")
public class HostsRest {

    final static Logger logger = LoggerFactory.getLogger(HostsRest.class);
    Database d = new Database();
    Hosts hosts;
    ObjectId userId;

    private boolean initUser(final HttpHeaders httpHeaders) {
        this.userId = new User().getUserId(httpHeaders);
        if (this.userId != null) {
            this.hosts = new Hosts(this.userId);
            return true;
        }
        return false;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Node> listHosts(@Context final HttpHeaders httpHeaders) {
        if (this.initUser(httpHeaders)) {
            try {
                final List<Node> list = Swarm.init().listNodes();
                return list;
            } catch (DockerException | InterruptedException | DockerCertificateException e) {
                throw new ApiException(e);
            }
        }
        throw new ApiException(Status.UNAUTHORIZED);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("new")
    public Response newHost(@javax.ws.rs.core.Context final HttpHeaders httpHeaders) {
        if (this.initUser(httpHeaders)) {
            final String command = Swarm.newHost();
            final String publicCommand = command.replace("sudo docker run",
                    "sudo docker run -e CATTLE_HOST_LABELS='type=public'");
            final String privateCommand = command.replace("sudo docker run",
                    "sudo docker run -e CATTLE_HOST_LABELS='type=private&user=" + this.userId + "'");
            ;
            return Response
                    .ok("{\"publicCommand\":\"" + publicCommand + "\", \"privateCommand\": \"" + privateCommand + "\"}")
                    .build();
        }
        return Response.status(Status.UNAUTHORIZED).build();
    }

    @POST
    @Path("{action}/{host}")
    public Response hostAction(@javax.ws.rs.core.Context final HttpHeaders httpHeaders,
            @PathParam("action") final String action,
            @PathParam("host") final String host) throws DockerCertificateException,
            DockerException,
            InterruptedException {
        HostsRest.logger.info("Host action: " + action + ", " + host);
        if (this.initUser(httpHeaders)) {
            final int status = Swarm.hostAction(host, action);
            HostsRest.logger.info("Rancher status: " + status);
            Swarm.syncHosts();
            return Response.status(status).build();
        }
        return Response.status(Status.UNAUTHORIZED).build();
    }
}
