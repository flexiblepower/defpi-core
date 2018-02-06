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
import org.flexiblepower.process.ProcessManager;
import org.json.JSONObject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessRestApi extends BaseApi implements ProcessApi {

    protected ProcessRestApi(@Context final HttpHeaders httpHeaders) {
        super(httpHeaders);
    }

    @Override
    public List<Process> listProcesses(final int page,
            final int perPage,
            final String sortDir,
            final String sortField,
            final String filters) throws AuthorizationException {
        // TODO Implement pagination
        List<Process> processes;
        if (this.sessionUser == null) {
            throw new AuthorizationException();
        } else if (this.sessionUser.isAdmin()) {
            processes = ProcessManager.getInstance().listProcesses();
        } else {
            processes = ProcessManager.getInstance().listProcesses(this.sessionUser);
        }
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
        return processes;
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