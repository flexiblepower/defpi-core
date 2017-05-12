package org.flexiblepower.rest;

import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;

import org.flexiblepower.api.ProcessApi;
import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.Service;
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
        return this.dockerConnector.listProcesses();
    }

    @Override
    public Process getProcess(final String uuid) throws ProcessNotFoundException {
        return this.dockerConnector.getProcess(uuid);
    }

    @Override
    public String newProcess(final Service service) {
        ProcessRestApi.log.info("newContainer(): " + service);
        return this.dockerConnector.newProcess(service, this.loggedInUser, null);
    }

    @Override
    public void removeProcess(final String uuid) throws ProcessNotFoundException {
        this.dockerConnector.removeProcess(uuid);
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