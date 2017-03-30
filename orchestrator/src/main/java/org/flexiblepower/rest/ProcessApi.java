package org.flexiblepower.rest;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.flexiblepower.exceptions.ApiException;
import org.flexiblepower.orchestrator.DockerConnector;

import lombok.extern.slf4j.Slf4j;

import io.swagger.annotations.Api;

@Slf4j
@Path("process")
@Api("Process")
@Produces(MediaType.APPLICATION_JSON)
public class ProcessApi extends BaseApi {

    protected ProcessApi(@Context final HttpHeaders httpHeaders) {
        super(httpHeaders);
    }

    private final DockerConnector dockerConnector = new DockerConnector();

    @GET
    public List<Process> listProcesses(@Context final HttpHeaders httpHeaders) {
        ProcessApi.log.info("REST");
        // this.initUser(httpHeaders);
        return this.dockerConnector.getProcesses();
    }

    @GET
    @Path("{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Process getProcess(@Context final HttpHeaders httpHeaders, @PathParam("uuid") final String uuid) {
        // this.initUser(httpHeaders);
        return this.dockerConnector.getProcess(uuid);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public String newProcess(@Context final HttpHeaders httpHeaders, final String json) {
        ProcessApi.log.info("newContainer(): " + json);

        try {
            // this.initUser(httpHeaders);
            return this.dockerConnector.newProcess(json);
        } catch (final Exception e) {
            ProcessApi.log.error(e.getMessage(), e);
            throw new ApiException(e);
        }
        // try {
        // final Gson gson = InitGson.create();
        // final ContainerDescription containerDescription = gson.fromJson(json, ContainerDescription.class);
        // if (this.containers.createContainer(containerDescription) == null) {
        // return Response.status(Status.FORBIDDEN).build();
        // }
        // } catch (final Exception e) {
        // ProcesApi.logger.error(e.toString());
        // return Response.status(Status.BAD_REQUEST).build();
        // }
        // return Response.ok().build();
    }

    // @DELETE
    // @Path("{uuid}")
    // public Response deleteContainer(@Context final HttpHeaders httpHeaders, @PathParam("uuid") final String uuid) {
    // ProcesApi.logger.info("deleteContainer('" + uuid + "')");
    // if (this.initUser(httpHeaders)) {
    // return Response.status(this.containers.deleteContainer(uuid)).build();
    // }
    // return Response.status(Status.UNAUTHORIZED).build();
    // }

    // @POST
    // @Path("{uuid}/upgrade")
    // public Response upgrade(@Context final HttpHeaders httpHeaders, @PathParam("uuid") final String uuid)
    // throws JsonSyntaxException,
    // DockerCertificateException,
    // DockerException,
    // InterruptedException {
    // ProcesApi.logger.info("Upgrade: " + uuid);
    // if (this.initUser(httpHeaders)) {
    // final Status status = this.containers.upgradeContainer(this.containers.getContainer(uuid));
    // return Response.status(status).build();
    // }
    // return Response.status(Status.UNAUTHORIZED).build();
    // }
}