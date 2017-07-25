package org.flexiblepower.rest;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;

import org.bson.types.ObjectId;
import org.flexiblepower.api.ProcessApi;
import org.flexiblepower.exceptions.ApiException;
import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.InvalidObjectIdException;
import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.model.Process;
import org.flexiblepower.orchestrator.MongoDbConnector;
import org.flexiblepower.orchestrator.ProcessManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessRestApi extends BaseApi implements ProcessApi {

    protected ProcessRestApi(@Context final HttpHeaders httpHeaders) {
        super(httpHeaders);
    }

    @Override
    public List<Process> listProcesses() {
        if (this.sessionUser == null) {
            return Collections.emptyList();
        } else if (this.sessionUser.isAdmin()) {
            return ProcessManager.getInstance().listProcesses();
        } else {
            return ProcessManager.getInstance().listProcesses(this.sessionUser);
        }
    }

    @Override
    public Process getProcess(final String id) throws ProcessNotFoundException,
            InvalidObjectIdException,
            AuthorizationException {
        final ObjectId oid = MongoDbConnector.stringToObjectId(id);
        final Process ret = ProcessManager.getInstance().getProcess(oid);
        if (ret == null) {
            throw new ProcessNotFoundException(id);
        }

        this.assertUserIsAdminOrEquals(ret.getUserId());

        return ret;
    }

    @Override
    public Process newProcess(final Process process) throws AuthorizationException {
        this.assertUserIsAdminOrEquals(process.getUserId());

        ProcessRestApi.log.info("Creating new process {}", process);
        return ProcessManager.getInstance().createProcess(process);
    }

    @Override
    public Process updateProcess(final String id, final Process process) throws AuthorizationException,
            InvalidObjectIdException,
            ProcessNotFoundException {
        // Immediately do all relevant checks...
        final Process currentProcess = this.getProcess(id);

        if (!currentProcess.getId().equals(process.getId())) {
            throw new ApiException(Status.FORBIDDEN, "Cannot change the ID of a process");
        }

        ProcessRestApi.log.info("Updating process {}", process);
        return ProcessManager.getInstance().updateProcess(process);
    }

    @Override
    public void removeProcess(final String id) throws ProcessNotFoundException,
            InvalidObjectIdException,
            AuthorizationException {
        // Immediately do all relevant checks...
        final Process process = this.getProcess(id);

        ProcessRestApi.log.info("Removing process {}", process);
        ProcessManager.getInstance().deleteProcess(process);
    }
}