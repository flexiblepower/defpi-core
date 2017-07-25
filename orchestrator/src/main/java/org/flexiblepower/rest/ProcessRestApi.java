package org.flexiblepower.rest;

import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

import org.bson.types.ObjectId;
import org.flexiblepower.api.ProcessApi;
import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.model.Process;
import org.flexiblepower.orchestrator.ProcessManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessRestApi extends BaseApi implements ProcessApi {

    // TODO authentication etc

    protected ProcessRestApi(@Context final HttpHeaders httpHeaders) {
        super(httpHeaders);
    }

    @Override
    public List<Process> listProcesses() {
        return ProcessManager.getInstance().listProcesses(); // TODO for the right user
    }

    @Override
    public Process getProcess(final String uuid) throws ProcessNotFoundException {
        return ProcessManager.getInstance().getProcess(new ObjectId(uuid));
    }

    @Override
    public Process newProcess(final Process process) {
        return ProcessManager.getInstance().createProcess(process);
    }

    @Override
    public Process updateProcess(final String uuid, final Process process) throws AuthorizationException {
        return ProcessManager.getInstance().updateProcess(process);
    }

    @Override
    public void removeProcess(final String uuid) throws ProcessNotFoundException {
        final ProcessManager pm = ProcessManager.getInstance();
        final Process process = pm.getProcess(new ObjectId(uuid));
        if (process == null) {
            throw new ProcessNotFoundException("Could not find process " + uuid);
        }
        pm.deleteProcess(process);
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