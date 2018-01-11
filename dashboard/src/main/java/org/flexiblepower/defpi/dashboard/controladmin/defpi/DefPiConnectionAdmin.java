package org.flexiblepower.defpi.dashboard.controladmin.defpi;

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

	private DefPiParameters parameters;
	private ProcessApi processApi;
	private ConnectionApi connApi;
	private ServiceApi serviceApi;

	private Map<String, Boolean> interfaceIdIsEfi = new HashMap<>();

	private Map<String, CemProcess> cems = new HashMap<>();
	private Map<String, RmProcess> rms = new HashMap<>();
	private Map<String, ObsPubProcess> obsPubs = new HashMap<>();
	private Map<String, ObsSubProcess> obsSubs = new HashMap<>();

	public DefPiConnectionAdmin(DefPiParameters parameters) {
		this.parameters = parameters;
		init();
		try {
			refreshData();
		} catch (UnsupportedEncodingException | AuthorizationException e) {
			LOG.warn("Was not able to get process and connection information", e);
		}
	}

	private void init() {
		processApi = JAXRSClientFactory.create(
				"http://" + stripURI(parameters.getOrchestratorHost()) + ":" + parameters.getOrchestratorPort(),
				ProcessApi.class, Arrays.asList(new JacksonJsonProvider()));
		connApi = JAXRSClientFactory.create(
				"http://" + stripURI(parameters.getOrchestratorHost()) + ":" + parameters.getOrchestratorPort(),
				ConnectionApi.class, Arrays.asList(new JacksonJsonProvider()));
		serviceApi = JAXRSClientFactory.create(
				"http://" + stripURI(parameters.getOrchestratorHost()) + ":" + parameters.getOrchestratorPort(),
				ServiceApi.class, Arrays.asList(new JacksonJsonProvider()));
		WebClient.client(processApi).header("X-Auth-Token", parameters.getOrchestratorToken());
		WebClient.client(connApi).header("X-Auth-Token", parameters.getOrchestratorToken());
		WebClient.client(serviceApi).header("X-Auth-Token", parameters.getOrchestratorToken());
	}

	private String stripURI(String uri) {
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

	private List<Process> findProcessesForHashpair(String hashpair) {
		JSONObject filter = new JSONObject();
		String encodedFilter = null;
		try {
			filter.put("hashpair", hashpair);
			encodedFilter = URLEncoder.encode(filter.toString(), "UTF-8");
			return processApi.listProcesses(encodedFilter);
		} catch (UnsupportedEncodingException | AuthorizationException e) {
			e.printStackTrace();
			return null;
		}
	}

	private boolean interfaceIdHasOneOfHashpairs(String interfaceId, String... hashpairs)
			throws AuthorizationException {
		String serviceId = interfaceId.split("/")[0];
		try {
			if (!interfaceIdIsEfi.containsKey(interfaceId)) {
				Service service = serviceApi.getService(serviceId);
				for (Interface i : service.getInterfaces()) {
					for (InterfaceVersion iv : i.getInterfaceVersions()) {
						String combined = iv.getSendsHash() + ";" + iv.getReceivesHash();
						for (String hashpair : hashpairs) {
							if (combined.equals(hashpair)) {
								interfaceIdIsEfi.put(interfaceId, true);
								return true;
							}
						}
					}
				}
				interfaceIdIsEfi.put(interfaceId, false);
			}
			return interfaceIdIsEfi.get(interfaceId);
		} catch (NotFoundException e) {
			LOG.warn("Could not find service with id " + serviceId);
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
	private Interface findCompatibleInterface(String serviceId, Interface otherServiceInterface)
			throws AuthorizationException, NotFoundException {
		Service service = serviceApi.getService(serviceId);
		for (Interface i : service.getInterfaces()) {
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
	private List<Interface> findMatchingInterface(String serviceId, String... hashpairs) throws AuthorizationException {
		List<Interface> result = new ArrayList<>();
		try {
			Service service = serviceApi.getService(serviceId);
			for (Interface i : service.getInterfaces()) {
				outer: for (InterfaceVersion iv : i.getInterfaceVersions()) {
					String combined = iv.getSendsHash() + ";" + iv.getReceivesHash();
					for (String hashpair : hashpairs) {
						if (combined.equals(hashpair)) {
							result.add(i);
							continue outer;
						}
					}
				}
			}
		} catch (NotFoundException e) {
			LOG.warn("Could not find service with id " + serviceId);
		}
		return result;
	}

	private Connection findConnectionForProcessesWithHashPair(DefPiProcess process1, DefPiProcess process2,
			String... hashpairs) throws UnsupportedEncodingException, AuthorizationException {
		JSONObject filter = new JSONObject();
		filter.put("processId", process1.getProcessId());
		String encodedFilter = URLEncoder.encode(filter.toString(), "UTF-8");
		for (Connection c : connApi.listConnections(encodedFilter)) {
			// Dirty little hack, getEndpointForProcess accepts a process
			Process p = new Process();
			p.setId(new ObjectId(process1.getProcessId()));
			Endpoint p1Endpoint = c.getEndpointForProcess(p);
			Endpoint p2Endpoint = c.getOtherEndpoint(p1Endpoint);
			if (p2Endpoint.getProcessId().toString().equals(process2.getProcessId())
					&& interfaceIdHasOneOfHashpairs(p1Endpoint.getInterfaceId(), hashpairs)) {
				// this is the connection
				return c;
			}
		}
		return null;
	}

	private List<String> findConnectedProcessWithHashpairs(Process p, String... hashpairs)
			throws UnsupportedEncodingException, AuthorizationException {
		List<String> res = new ArrayList<>();
		JSONObject filter = new JSONObject();
		filter.put("processId", p.getId());
		String encodedFilter = URLEncoder.encode(filter.toString(), "UTF-8");
		List<Connection> listConnections = connApi.listConnections(encodedFilter);
		for (Connection c : listConnections) {
			Endpoint otherEndpoint = c.getOtherEndpoint(c.getEndpointForProcess(p));
			if (interfaceIdHasOneOfHashpairs(c.getEndpoint1().getInterfaceId(), hashpairs)) {
				res.add(otherEndpoint.getProcessId().toString());
			}
		}
		return res;
	}

	public void refreshData() throws UnsupportedEncodingException, AuthorizationException {
		cems.clear();
		for (Process p : findProcessesForHashpair(CEM_INFLEX_HASHPAIR)) {
			cems.put(p.getId().toString(), new CemProcess(this, p.getId().toString(), p.getServiceId()));
		}
		rms.clear();
		for (Process p : findProcessesForHashpair(RM_INFLEX_HASHPAIR)) {
			RmProcess newRm = new RmProcess(this, p.getId().toString(), p.getServiceId());
			rms.put(p.getId().toString(), newRm);
			for (String cemProcessId : findConnectedProcessWithHashpairs(p, CEM_INFLEX_HASHPAIR, RM_INFLEX_HASHPAIR)) {
				// connect the models
				cems.get(cemProcessId).getRmProcessIds().add(newRm.getProcessId());
				newRm.setCemProcessId(cemProcessId);
			}
		}
		obsPubs.clear();
		for (Process p : findProcessesForHashpair(OBSERVATION_PUB_HASHPAIR)) {
			obsPubs.put(p.getId().toString(), new ObsPubProcess(this, p.getId().toString(), p.getServiceId()));
		}
		obsSubs.clear();
		for (Process p : findProcessesForHashpair(OBSERVATION_SUB_HASHPAIR)) {
			ObsSubProcess newObsSub = new ObsSubProcess(this, p.getId().toString(), p.getServiceId());
			obsSubs.put(p.getId().toString(), newObsSub);
			for (String obsPubProcessId : findConnectedProcessWithHashpairs(p, CEM_INFLEX_HASHPAIR,
					RM_INFLEX_HASHPAIR)) {
				// connect the models
				ObsPubProcess obsPubProcess = obsPubs.get(obsPubProcessId);
				obsPubProcess.getSubscriberProcessIds().add(newObsSub.getProcessId());
				newObsSub.getPublisherProcessIds().add(obsPubProcessId);
			}
		}
	}

	public Collection<CemProcess> listCems() {
		return cems.values();
	}

	public CemProcess getCemProcess(String id) {
		return cems.get(id);
	}

	public Collection<RmProcess> listRms() {
		return rms.values();
	}

	public RmProcess getRmProcess(String id) {
		return rms.get(id);
	}

	public Collection<ObsPubProcess> listObsPub() {
		return obsPubs.values();
	}

	public ObsPubProcess getObsPubProcess(String id) {
		return obsPubs.get(id);
	}

	public Collection<ObsSubProcess> listObsSub() {
		return obsSubs.values();
	}

	public ObsSubProcess getObsSubProcess(String id) {
		return obsSubs.get(id);
	}

	public void connect(CemProcess cemProcess, RmProcess rmProcess)
			throws AuthorizationException, ConnectionException, NotFoundException, IOException {
		if (rmProcess.isConnectedWith(cemProcess)) {
			// Nothing to do here
			return;
		}
		Interface rmInterface = findMatchingInterface(rmProcess.getServiceId(), RM_INFLEX_HASHPAIR).get(0);
		Interface cemInterfaceId = findCompatibleInterface(cemProcess.getServiceId(), rmInterface);
		connApi.newConnection(new Connection(new ObjectId(),
				new Endpoint(new ObjectId(cemProcess.getProcessId()), cemInterfaceId.getId()),
				new Endpoint(new ObjectId(rmProcess.getProcessId()), rmInterface.getId())));
		// update model
		rmProcess.setCemProcessId(cemProcess.getProcessId());
		cemProcess.getRmProcessIds().add(rmProcess.getProcessId());
		LOG.info("Created EFI connection between CEM " + cemProcess.getProcessId() + " and RM "
				+ rmProcess.getProcessId());
	}

	public void disconnect(CemProcess cemProcess, RmProcess rmProcess)
			throws UnsupportedEncodingException, AuthorizationException, NotFoundException, InvalidObjectIdException {
		if (!rmProcess.isConnectedWith(cemProcess)) {
			// Nothing to do here
			return;
		}
		Connection c = findConnectionForProcessesWithHashPair(cemProcess, rmProcess, CEM_INFLEX_HASHPAIR,
				RM_INFLEX_HASHPAIR);
		if (c == null) {
			LOG.warn("Should disconnect cem " + cemProcess.getProcessId() + " from rm " + rmProcess.getProcessId()
					+ ", but didn't find valid a connection");
		} else {
			connApi.deleteConnection(c.getId().toString());
			// Update the model
			cemProcess.getRmProcessIds().remove(rmProcess.getProcessId());
			rmProcess.setCemProcessId(null);
			LOG.info("Deleted EFI connection between CEM " + cemProcess.getProcessId() + " and RM "
					+ rmProcess.getProcessId());
		}
	}

	public void connect(ObsPubProcess obsPubProcess, ObsSubProcess obsSubProcess)
			throws ConnectionException, AuthorizationException, NotFoundException {
		if (obsPubProcess.isConnectedWith(obsSubProcess)) {
			// Nothing to do here
			return;
		}
		Interface obsPubInterface = findMatchingInterface(obsPubProcess.getServiceId(), OBSERVATION_PUB_HASHPAIR)
				.get(0);
		Interface obsSubInterfaceId = findCompatibleInterface(obsSubProcess.getServiceId(), obsPubInterface);
		connApi.newConnection(new Connection(new ObjectId(),
				new Endpoint(new ObjectId(obsSubProcess.getProcessId()), obsSubInterfaceId.getId()),
				new Endpoint(new ObjectId(obsPubProcess.getProcessId()), obsPubInterface.getId())));
		// update model
		obsPubProcess.getSubscriberProcessIds().add(obsSubProcess.getProcessId());
		obsSubProcess.getPublisherProcessIds().add(obsPubProcess.getProcessId());
		LOG.info("Created Observations connection between publisher " + obsPubProcess.getProcessId()
				+ " and subscriber " + obsSubProcess.getProcessId());
	}

	public void disconnect(ObsPubProcess obsPubProcess, ObsSubProcess obsSubProcess)
			throws UnsupportedEncodingException, AuthorizationException, NotFoundException, InvalidObjectIdException {
		if (!obsPubProcess.isConnectedWith(obsSubProcess)) {
			// Nothing to do here
			return;
		}
		Connection c = findConnectionForProcessesWithHashPair(obsPubProcess, obsSubProcess, OBSERVATION_PUB_HASHPAIR,
				OBSERVATION_SUB_HASHPAIR);
		if (c == null) {
			LOG.warn("Should disconnect observation publisher " + obsPubProcess.getProcessId()
					+ " from observation subscriber " + obsSubProcess.getProcessId()
					+ ", but didn't find valid a connection");
		} else {
			connApi.deleteConnection(c.getId().toString());
			// Update the model
			obsPubProcess.getSubscriberProcessIds().remove(obsSubProcess.getProcessId());
			obsSubProcess.getPublisherProcessIds().remove(obsPubProcess.getProcessId());
			LOG.info("Deleted connection between observation publisher " + obsPubProcess.getProcessId()
					+ " and observation subscriber " + obsSubProcess.getProcessId());
		}
	}

}
