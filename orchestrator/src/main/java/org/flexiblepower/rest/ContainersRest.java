package org.flexiblepower.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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

import org.bson.Document;
import org.bson.types.ObjectId;
import org.flexiblepower.gson.ContainerDescription;
import org.flexiblepower.gson.InitGson;
import org.flexiblepower.model.User;
import org.flexiblepower.orchestrator.Containers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;

@Path("containers")
public class ContainersRest {

    public final static Logger logger = LoggerFactory.getLogger(ContainersRest.class);
    Containers containers;

    private boolean initUser(final HttpHeaders httpHeaders) {
        final ObjectId userId = new User().getUserId(httpHeaders);
        if (userId != null) {
            this.containers = new Containers(userId);
            return true;
        }
        return false;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listContainers(@Context final HttpHeaders httpHeaders) {
        ContainersRest.logger.info("REST");
        if (this.initUser(httpHeaders)) {
            return Response.ok(new Document("containers", this.containers.listContainers()).toJson()).build();
        }
        return Response.status(Status.UNAUTHORIZED).build();
    }

    @GET
    @Path("{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listContainer(@Context final HttpHeaders httpHeaders, @PathParam("uuid") final String uuid) {
        if (this.initUser(httpHeaders)) {
            return Response.ok(this.containers.getContainer(uuid).toJson()).build();
        }
        return Response.status(Status.UNAUTHORIZED).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response newContainer(@Context final HttpHeaders httpHeaders, final String json) {
        ContainersRest.logger.info("newContainer(): " + json);
        if (this.initUser(httpHeaders)) {
            try {
                final Gson gson = InitGson.create();
                final ContainerDescription containerDescription = gson.fromJson(json, ContainerDescription.class);
                if (this.containers.createContainer(containerDescription) == null) {
                    return Response.status(Status.FORBIDDEN).build();
                }
            } catch (final Exception e) {
                ContainersRest.logger.error(e.toString());
                return Response.status(Status.BAD_REQUEST).build();
            }
            return Response.ok().build();
        }
        return Response.status(Status.UNAUTHORIZED).build();
    }

    @DELETE
    @Path("{uuid}")
    public Response deleteContainer(@Context final HttpHeaders httpHeaders, @PathParam("uuid") final String uuid) {
        ContainersRest.logger.info("deleteContainer('" + uuid + "')");
        if (this.initUser(httpHeaders)) {
            return Response.status(this.containers.deleteContainer(uuid)).build();
        }
        return Response.status(Status.UNAUTHORIZED).build();
    }

    @POST
    @Path("{uuid}/upgrade")
    public Response upgrade(@Context final HttpHeaders httpHeaders, @PathParam("uuid") final String uuid)
            throws JsonSyntaxException,
            DockerCertificateException,
            DockerException,
            InterruptedException {
        ContainersRest.logger.info("Upgrade: " + uuid);
        if (this.initUser(httpHeaders)) {
            final Status status = this.containers.upgradeContainer(this.containers.getContainer(uuid));
            return Response.status(status).build();
        }
        return Response.status(Status.UNAUTHORIZED).build();
    }
}