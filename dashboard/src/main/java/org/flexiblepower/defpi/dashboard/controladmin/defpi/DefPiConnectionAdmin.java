package org.flexiblepower.defpi.dashboard.controladmin.defpi;

/*-
 * #%L
 * dEF-Pi dashboard
 * %%
 * Copyright (C) 2017 - 2018 Flexible Power Alliance Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.bson.types.ObjectId;
import org.flexiblepower.api.ConnectionApi;
import org.flexiblepower.api.ProcessApi;
import org.flexiblepower.api.ServiceApi;
import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.ConnectionException;
import org.flexiblepower.exceptions.InvalidObjectIdException;
import org.flexiblepower.exceptions.NotFoundException;
import org.flexiblepower.model.Connection;
import org.flexiblepower.model.Connection.Endpoint;
import org.flexiblepower.model.Interface;
import org.flexiblepower.model.InterfaceVersion;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.Service;
import org.flexiblepower.service.DefPiParameters;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

public class DefPiConnectionAdmin {

    public static final Logger LOG = LoggerFactory.getLogger(DefPiConnectionAdmin.class);

    public static String CEM_INFLEX_HASHPAIR = "ebb3d8802b40e69e654dab6645ec80337754cc5ad984dce9fa7ccad24a895ee4;7e2febcc8bceea08b8a64bb6482fd246f0ccf0d25bca8a91d30d0c27f826d8f6";
    public static String RM_INFLEX_HASHPAIR = "7e2febcc8bceea08b8a64bb6482fd246f0ccf0d25bca8a91d30d0c27f826d8f6;ebb3d8802b40e69e654dab6645ec80337754cc5ad984dce9fa7ccad24a895ee4";
    public static String OBSERVATION_PUB_HASHPAIR = "7e47e127724e6fb167f9770fafb13e25f7e3cff1e2f444795e0548b19610b93;fc2e6cb9984c76e3c7c8a036dd2fd4d466a2ef47351d7f4f435b5c106505d28b";
    public static String OBSERVATION_SUB_HASHPAIR = "fc2e6cb9984c76e3c7c8a036dd2fd4d466a2ef47351d7f4f435b5c106505d28b;7e47e127724e6fb167f9770fafb13e25f7e3cff1e2f444795e0548b19610b93";

    private final DefPiParameters parameters;
    private ProcessApi processApi;
    private ConnectionApi connApi;
    private ServiceApi serviceApi;

    private final Map<String, Boolean> interfaceIdIsEfi = new HashMap<>();

    private final Map<String, CemProcess> cems = new HashMap<>();
    private final Map<String, RmProcess> rms = new HashMap<>();
    private final Map<String, ObsPubProcess> obsPubs = new HashMap<>();
    private final Map<String, ObsSubProcess> obsSubs = new HashMap<>();

    public DefPiConnectionAdmin(final DefPiParameters parameters) {
        this.parameters = parameters;
        this.init();
        try {
            this.refreshData();
        } catch (UnsupportedEncodingException | AuthorizationException e) {
            DefPiConnectionAdmin.LOG.warn("Was not able to get process and connection information", e);
        }
    }

    private void init() {
        this.processApi = JAXRSClientFactory.create(
                "http://" + DefPiConnectionAdmin.stripURI(this.parameters.getOrchestratorHost()) + ":"
                        + this.parameters.getOrchestratorPort(),
                ProcessApi.class,
                Arrays.asList(new JacksonJsonProvider()));
        this.connApi = JAXRSClientFactory.create(
                "http://" + DefPiConnectionAdmin.stripURI(this.parameters.getOrchestratorHost()) + ":"
                        + this.parameters.getOrchestratorPort(),
                ConnectionApi.class,
                Arrays.asList(new JacksonJsonProvider()));
        this.serviceApi = JAXRSClientFactory.create(
                "http://" + DefPiConnectionAdmin.stripURI(this.parameters.getOrchestratorHost()) + ":"
                        + this.parameters.getOrchestratorPort(),
                ServiceApi.class,
                Arrays.asList(new JacksonJsonProvider()));
        WebClient.client(this.processApi).header("X-Auth-Token", this.parameters.getOrchestratorToken());
        WebClient.client(this.connApi).header("X-Auth-Token", this.parameters.getOrchestratorToken());
        WebClient.client(this.serviceApi).header("X-Auth-Token", this.parameters.getOrchestratorToken());
    }

    private static String stripURI(final String uri) {
        int begin = 0;
        int end = uri.length();
        if (uri.startsWith("/")) {
            begin = 1;
            end = uri.length();
        }
        if (uri.endsWith("/")) {
            end = Math.max(uri.length() - 1, 1);
        }
        return uri.substring(begin, end);
    }

    private List<Process> findProcessesForHashpair(final String hashpair) {
        final JSONObject filter = new JSONObject();
        String encodedFilter = null;
        try {
            filter.put("hashpair", hashpair);
            encodedFilter = URLEncoder.encode(filter.toString(), "UTF-8");
            return this.processApi.listProcesses(0, 0, "", "", encodedFilter);
        } catch (UnsupportedEncodingException | AuthorizationException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean interfaceIdHasOneOfHashpairs(final String interfaceId, final String... hashpairs)
            throws AuthorizationException {
        final String serviceId = interfaceId.split("/")[0];
        try {
            if (!this.interfaceIdIsEfi.containsKey(interfaceId)) {
                final Service service = this.serviceApi.getService(serviceId);
                for (final Interface i : service.getInterfaces()) {
                    for (final InterfaceVersion iv : i.getInterfaceVersions()) {
                        final String combined = iv.getSendsHash() + ";" + iv.getReceivesHash();
                        for (final String hashpair : hashpairs) {
                            if (combined.equals(hashpair)) {
                                this.interfaceIdIsEfi.put(interfaceId, true);
                                return true;
                            }
                        }
                    }
                }
                this.interfaceIdIsEfi.put(interfaceId, false);
            }
            return this.interfaceIdIsEfi.get(interfaceId);
        } catch (final NotFoundException e) {
            DefPiConnectionAdmin.LOG.warn("Could not find service with id " + serviceId);
            return false;
        }
    }

    /**
     * Find an interface of a given service which is compatible (can connect with) a
     * given interface from another service.
     *
     * @param serviceId
     * @param otherServiceInterface
     * @return
     * @throws AuthorizationException
     * @throws NotFoundException
     */
    private Interface findCompatibleInterface(final String serviceId, final Interface otherServiceInterface)
            throws AuthorizationException,
            NotFoundException {
        final Service service = this.serviceApi.getService(serviceId);
        for (final Interface i : service.getInterfaces()) {
            if (i.isCompatibleWith(otherServiceInterface)) {
                return i;
            }
        }
        return null;
    }

    /**
     * Find all interfaces of a service which have an interfaceVersion for any of
     * the set of given hashpairs.
     *
     * @param serviceId
     * @param hashpairs
     * @return
     * @throws AuthorizationException
     */
    private List<Interface> findMatchingInterface(final String serviceId, final String... hashpairs)
            throws AuthorizationException {
        final List<Interface> result = new ArrayList<>();
        try {
            final Service service = this.serviceApi.getService(serviceId);
            for (final Interface i : service.getInterfaces()) {
                outer: for (final InterfaceVersion iv : i.getInterfaceVersions()) {
                    final String combined = iv.getSendsHash() + ";" + iv.getReceivesHash();
                    for (final String hashpair : hashpairs) {
                        if (combined.equals(hashpair)) {
                            result.add(i);
                            continue outer;
                        }
                    }
                }
            }
        } catch (final NotFoundException e) {
            DefPiConnectionAdmin.LOG.warn("Could not find service with id " + serviceId);
        }
        return result;
    }

    private Connection findConnectionForProcessesWithHashPair(final DefPiProcess process1,
            final DefPiProcess process2,
            final String... hashpairs) throws UnsupportedEncodingException,
            AuthorizationException {
        final JSONObject filter = new JSONObject();
        filter.put("processId", process1.getProcessId());
        final String encodedFilter = URLEncoder.encode(filter.toString(), "UTF-8");
        for (final Connection c : this.connApi.listConnections(0, 0, "", "", encodedFilter)) {
            // Dirty little hack, getEndpointForProcess accepts a process
            final Process p = new Process();
            p.setId(new ObjectId(process1.getProcessId()));
            final Endpoint p1Endpoint = c.getEndpointForProcess(p);
            final Endpoint p2Endpoint = c.getOtherEndpoint(p1Endpoint);
            if (p2Endpoint.getProcessId().toString().equals(process2.getProcessId())
                    && this.interfaceIdHasOneOfHashpairs(p1Endpoint.getInterfaceId(), hashpairs)) {
                // this is the connection
                return c;
            }
        }
        return null;
    }

    private List<String> findConnectedProcessWithHashpairs(final Process p, final String... hashpairs)
            throws UnsupportedEncodingException,
            AuthorizationException {
        final List<String> res = new ArrayList<>();
        final JSONObject filter = new JSONObject();
        filter.put("processId", p.getId());
        final String encodedFilter = URLEncoder.encode(filter.toString(), "UTF-8");
        final List<Connection> listConnections = this.connApi.listConnections(0, 0, "", "", encodedFilter);
        for (final Connection c : listConnections) {
            final Endpoint otherEndpoint = c.getOtherEndpoint(c.getEndpointForProcess(p));
            if (this.interfaceIdHasOneOfHashpairs(c.getEndpoint1().getInterfaceId(), hashpairs)) {
                res.add(otherEndpoint.getProcessId().toString());
            }
        }
        return res;
    }

    public void refreshData() throws UnsupportedEncodingException, AuthorizationException {
        this.cems.clear();
        for (final Process p : this.findProcessesForHashpair(DefPiConnectionAdmin.CEM_INFLEX_HASHPAIR)) {
            this.cems.put(p.getId().toString(), new CemProcess(this, p.getId().toString(), p.getServiceId()));
        }
        this.rms.clear();
        for (final Process p : this.findProcessesForHashpair(DefPiConnectionAdmin.RM_INFLEX_HASHPAIR)) {
            final RmProcess newRm = new RmProcess(this, p.getId().toString(), p.getServiceId());
            this.rms.put(p.getId().toString(), newRm);
            for (final String cemProcessId : this.findConnectedProcessWithHashpairs(p,
                    DefPiConnectionAdmin.CEM_INFLEX_HASHPAIR,
                    DefPiConnectionAdmin.RM_INFLEX_HASHPAIR)) {
                // connect the models
                this.cems.get(cemProcessId).getRmProcessIds().add(newRm.getProcessId());
                newRm.setCemProcessId(cemProcessId);
            }
        }
        this.obsPubs.clear();
        for (final Process p : this.findProcessesForHashpair(DefPiConnectionAdmin.OBSERVATION_PUB_HASHPAIR)) {
            this.obsPubs.put(p.getId().toString(), new ObsPubProcess(this, p.getId().toString(), p.getServiceId()));
        }
        this.obsSubs.clear();
        for (final Process p : this.findProcessesForHashpair(DefPiConnectionAdmin.OBSERVATION_SUB_HASHPAIR)) {
            final ObsSubProcess newObsSub = new ObsSubProcess(this, p.getId().toString(), p.getServiceId());
            this.obsSubs.put(p.getId().toString(), newObsSub);
            for (final String obsPubProcessId : this.findConnectedProcessWithHashpairs(p,
                    DefPiConnectionAdmin.CEM_INFLEX_HASHPAIR,
                    DefPiConnectionAdmin.RM_INFLEX_HASHPAIR)) {
                // connect the models
                final ObsPubProcess obsPubProcess = this.obsPubs.get(obsPubProcessId);
                obsPubProcess.getSubscriberProcessIds().add(newObsSub.getProcessId());
                newObsSub.getPublisherProcessIds().add(obsPubProcessId);
            }
        }
    }

    public Collection<CemProcess> listCems() {
        return this.cems.values();
    }

    public CemProcess getCemProcess(final String id) {
        return this.cems.get(id);
    }

    public Collection<RmProcess> listRms() {
        return this.rms.values();
    }

    public RmProcess getRmProcess(final String id) {
        return this.rms.get(id);
    }

    public Collection<ObsPubProcess> listObsPub() {
        return this.obsPubs.values();
    }

    public ObsPubProcess getObsPubProcess(final String id) {
        return this.obsPubs.get(id);
    }

    public Collection<ObsSubProcess> listObsSub() {
        return this.obsSubs.values();
    }

    public ObsSubProcess getObsSubProcess(final String id) {
        return this.obsSubs.get(id);
    }

    public void connect(final CemProcess cemProcess, final RmProcess rmProcess) throws AuthorizationException,
            ConnectionException,
            NotFoundException,
            IOException {
        if (rmProcess.isConnectedWith(cemProcess)) {
            // Nothing to do here
            return;
        }
        final Interface rmInterface = this
                .findMatchingInterface(rmProcess.getServiceId(), DefPiConnectionAdmin.RM_INFLEX_HASHPAIR)
                .get(0);
        final Interface cemInterfaceId = this.findCompatibleInterface(cemProcess.getServiceId(), rmInterface);
        this.connApi.newConnection(new Connection(new ObjectId(),
                new Endpoint(new ObjectId(cemProcess.getProcessId()), cemInterfaceId.getId()),
                new Endpoint(new ObjectId(rmProcess.getProcessId()), rmInterface.getId())));
        // update model
        rmProcess.setCemProcessId(cemProcess.getProcessId());
        cemProcess.getRmProcessIds().add(rmProcess.getProcessId());
        DefPiConnectionAdmin.LOG.info("Created EFI connection between CEM " + cemProcess.getProcessId() + " and RM "
                + rmProcess.getProcessId());
    }

    public void disconnect(final CemProcess cemProcess, final RmProcess rmProcess) throws UnsupportedEncodingException,
            AuthorizationException,
            NotFoundException,
            InvalidObjectIdException {
        if (!rmProcess.isConnectedWith(cemProcess)) {
            // Nothing to do here
            return;
        }
        final Connection c = this.findConnectionForProcessesWithHashPair(cemProcess,
                rmProcess,
                DefPiConnectionAdmin.CEM_INFLEX_HASHPAIR,
                DefPiConnectionAdmin.RM_INFLEX_HASHPAIR);
        if (c == null) {
            DefPiConnectionAdmin.LOG.warn("Should disconnect cem " + cemProcess.getProcessId() + " from rm "
                    + rmProcess.getProcessId() + ", but didn't find valid a connection");
        } else {
            this.connApi.deleteConnection(c.getId().toString());
            // Update the model
            cemProcess.getRmProcessIds().remove(rmProcess.getProcessId());
            rmProcess.setCemProcessId(null);
            DefPiConnectionAdmin.LOG.info("Deleted EFI connection between CEM " + cemProcess.getProcessId() + " and RM "
                    + rmProcess.getProcessId());
        }
    }

    public void connect(final ObsPubProcess obsPubProcess, final ObsSubProcess obsSubProcess)
            throws ConnectionException,
            AuthorizationException,
            NotFoundException {
        if (obsPubProcess.isConnectedWith(obsSubProcess)) {
            // Nothing to do here
            return;
        }
        final Interface obsPubInterface = this
                .findMatchingInterface(obsPubProcess.getServiceId(), DefPiConnectionAdmin.OBSERVATION_PUB_HASHPAIR)
                .get(0);
        final Interface obsSubInterfaceId = this.findCompatibleInterface(obsSubProcess.getServiceId(), obsPubInterface);
        this.connApi.newConnection(new Connection(new ObjectId(),
                new Endpoint(new ObjectId(obsSubProcess.getProcessId()), obsSubInterfaceId.getId()),
                new Endpoint(new ObjectId(obsPubProcess.getProcessId()), obsPubInterface.getId())));
        // update model
        obsPubProcess.getSubscriberProcessIds().add(obsSubProcess.getProcessId());
        obsSubProcess.getPublisherProcessIds().add(obsPubProcess.getProcessId());
        DefPiConnectionAdmin.LOG.info("Created Observations connection between publisher "
                + obsPubProcess.getProcessId() + " and subscriber " + obsSubProcess.getProcessId());
    }

    public void disconnect(final ObsPubProcess obsPubProcess, final ObsSubProcess obsSubProcess)
            throws UnsupportedEncodingException,
            AuthorizationException,
            NotFoundException,
            InvalidObjectIdException {
        if (!obsPubProcess.isConnectedWith(obsSubProcess)) {
            // Nothing to do here
            return;
        }
        final Connection c = this.findConnectionForProcessesWithHashPair(obsPubProcess,
                obsSubProcess,
                DefPiConnectionAdmin.OBSERVATION_PUB_HASHPAIR,
                DefPiConnectionAdmin.OBSERVATION_SUB_HASHPAIR);
        if (c == null) {
            DefPiConnectionAdmin.LOG.warn("Should disconnect observation publisher " + obsPubProcess.getProcessId()
                    + " from observation subscriber " + obsSubProcess.getProcessId()
                    + ", but didn't find valid a connection");
        } else {
            this.connApi.deleteConnection(c.getId().toString());
            // Update the model
            obsPubProcess.getSubscriberProcessIds().remove(obsSubProcess.getProcessId());
            obsSubProcess.getPublisherProcessIds().remove(obsPubProcess.getProcessId());
            DefPiConnectionAdmin.LOG.info("Deleted connection between observation publisher "
                    + obsPubProcess.getProcessId() + " and observation subscriber " + obsSubProcess.getProcessId());
        }
    }

}
