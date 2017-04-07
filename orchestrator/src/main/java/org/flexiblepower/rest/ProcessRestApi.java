package org.flexiblepower.rest;

import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;

import org.flexiblepower.api.ProcessApi;
import org.flexiblepower.orchestrator.DockerConnector;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessRestApi extends BaseApi implements ProcessApi {

    protected ProcessRestApi(@Context final HttpHeaders httpHeaders, @Context final SecurityContext securityContext) {
        super(httpHeaders, securityContext);
    }

    private final DockerConnector dockerConnector = new DockerConnector();

    @Override
    public List<Process> listProcesses() {
        ProcessRestApi.log.info("REST");
        return this.dockerConnector.getProcesses();
    }

    @Override
    public Process getProcess(final String uuid) {
        return this.dockerConnector.getProcess(uuid);
    }

    @Override
    public String newProcess(final String json) {
        ProcessRestApi.log.info("newContainer(): " + json);
        return this.dockerConnector.newProcess(json);
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