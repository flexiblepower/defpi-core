/**
 * File ProcessRestApi.java
 *
 * Copyright 2017 FAN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.flexiblepower.rest;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;

import org.bson.types.ObjectId;
import org.flexiblepower.api.ProcessApi;
import org.flexiblepower.connectors.MongoDbConnector;
import org.flexiblepower.exceptions.ApiException;
import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.InvalidObjectIdException;
import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.Interface;
import org.flexiblepower.model.InterfaceVersion;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.Service;
import org.flexiblepower.orchestrator.ServiceManager;
import org.flexiblepower.orchestrator.UserManager;
import org.flexiblepower.process.ProcessManager;
import org.json.JSONObject;

import lombok.extern.slf4j.Slf4j;

/**
 * ProcessRestApi
 *
 * @version 0.1
 * @since Mar 30, 2017
 */
@Slf4j
public class ProcessRestApi extends BaseApi implements ProcessApi {

    /**
     * Create the REST API with the headers from the HTTP request (will be injected by the HTTP server)
     *
     * @param httpHeaders The headers from the HTTP request for authorization
     */
    protected ProcessRestApi(@Context final HttpHeaders httpHeaders) {
        super(httpHeaders);
    }

    @Override
    public List<Process> listProcesses(final int page,
            final int perPage,
            final String sortDir,
            final String sortField,
            final String filters) throws AuthorizationException {
        if ((page < 1) || (perPage < 1)) {
            return Collections.emptyList();
        }

        List<Process> processes;
        if (this.sessionUser == null) {
            throw new AuthorizationException();
        } else if (this.sessionUser.isAdmin()) {
            processes = ProcessManager.getInstance().listProcesses();
        } else {
            processes = ProcessManager.getInstance().listProcessesForUser(this.sessionUser);
        }

        // Filters are a bit custom
        if (filters != null) {
            final JSONObject f = new JSONObject(filters);
            if (f.has("hashpair") && f.getString("hashpair").contains(";")) {
                final String[] split = f.getString("hashpair").split(";");
                final Iterator<Process> it = processes.iterator();
                while (it.hasNext()) {
                    final Process p = it.next();
                    boolean matches = false;
                    try {
                        final Service s = ServiceManager.getInstance().getService(p.getServiceId());
                        outerloop: for (final Interface itfs : s.getInterfaces()) {
                            for (final InterfaceVersion itfsv : itfs.getInterfaceVersions()) {
                                if (itfsv.getSendsHash().equals(split[0]) && itfsv.getReceivesHash().equals(split[1])) {
                                    matches = true;
                                    break outerloop;
                                }
                            }
                        }
                    } catch (final ServiceNotFoundException e) {
                        // ignore
                    }
                    if (!matches) {
                        it.remove();
                    }
                }
            }
            if (f.has("userId")) {
                final Iterator<Process> it = processes.iterator();
                while (it.hasNext()) {
                    if (!it.next().getUserId().toString().equals(f.getString("userId"))) {
                        it.remove();
                    }
                }
            }
        }

        // Now do the sorting
        final Comparator<Process> comparator;
        switch (sortField) {
        case "userId":
            comparator = (one, other) -> UserManager.getInstance().getUser(one.getUserId()).getUsername().compareTo(
                    UserManager.getInstance().getUser(other.getUserId()).getUsername());
            break;
        case "serviceId":
            comparator = (one, other) -> one.getServiceId().compareTo(other.getServiceId());
            break;
        case "state":
            comparator = (one, other) -> one.getState().toString().compareTo(other.getState().toString());
            break;
        case "Id":
        default:
            comparator = (one, other) -> one.getId().toString().compareTo(other.getId().toString());
            break;
        }
        Collections.sort(processes, comparator);

        // Order the sorting if necessary
        if (sortDir.equals("DESC")) {
            Collections.reverse(processes);
        }

        // And finally pagination
        return processes.subList(Math.min(processes.size(), (page - 1) * perPage),
                Math.min(processes.size(), page * perPage));
    }

    @Override
    public Process getProcess(final String id) throws ProcessNotFoundException,
            InvalidObjectIdException,
            AuthorizationException {
        final ObjectId oid = MongoDbConnector.stringToObjectId(id);
        final Process ret = ProcessManager.getInstance().getProcess(oid);
        if (ret == null) {
            throw new ProcessNotFoundException(oid);
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
        ProcessManager.getInstance().updateProcess(process);
        return process;
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

    @Override
    public void triggerProcessConfig(final String id) throws ProcessNotFoundException,
            InvalidObjectIdException,
            AuthorizationException {
        // Immediately do all relevant checks...
        final Process currentProcess = this.getProcess(id);

        ProcessManager.getInstance().triggerConfig(currentProcess);
    }
}