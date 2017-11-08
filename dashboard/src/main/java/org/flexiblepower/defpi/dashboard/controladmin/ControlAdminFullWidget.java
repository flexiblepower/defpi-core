package org.flexiblepower.defpi.dashboard.controladmin;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.flexiblepower.api.ConnectionApi;
import org.flexiblepower.api.ProcessApi;
import org.flexiblepower.api.ServiceApi;
import org.flexiblepower.defpi.dashboard.Dashboard;
import org.flexiblepower.defpi.dashboard.Widget;
import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPRequest;
import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPRequest.Method;
import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPResponse;
import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.NotFoundException;
import org.flexiblepower.model.Interface;
import org.flexiblepower.model.InterfaceVersion;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.Service;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.protobuf.ByteString;

/**
 * @author subramaniana
 */
public class ControlAdminFullWidget implements Widget {

	public static final Logger LOG = LoggerFactory.getLogger(ControlAdminFullWidget.class);
	private final Dashboard service;
	private ConnectionApi connApi;
	private ProcessApi processApi;
	private ServiceApi serviceApi;
	private HashMap<Process, List<org.flexiblepower.model.Connection>> cemProcessMap;
	private HashMap<Process, List<org.flexiblepower.model.Connection>> rmProcessMap;
	private HashMap<Process, List<org.flexiblepower.model.Connection>> obsConProcessMap;
	private HashMap<Process, List<org.flexiblepower.model.Connection>> obsPubProcessMap;

	/**
	 * Auto-generated constructor for the ConnectionHandlers of the provided service
	 *
	 * @param service
	 *            The service for which to handle the connections
	 */
	public ControlAdminFullWidget(Dashboard service) {
		this.service = service;
		processApi = JAXRSClientFactory.create(
				"http://" + stripURI(service.getParameters().getOrchestratorHost()) + ":"
						+ service.getParameters().getOrchestratorPort(),
				ProcessApi.class, Arrays.asList(new JacksonJsonProvider()));
		connApi = JAXRSClientFactory.create(
				"http://" + stripURI(service.getParameters().getOrchestratorHost()) + ":"
						+ service.getParameters().getOrchestratorPort(),
				ConnectionApi.class, Arrays.asList(new JacksonJsonProvider()));
		serviceApi = JAXRSClientFactory.create(
				"http://" + stripURI(service.getParameters().getOrchestratorHost()) + ":"
						+ service.getParameters().getOrchestratorPort(),
				ServiceApi.class, Arrays.asList(new JacksonJsonProvider()));
		WebClient.client(processApi).header("X-Auth-Token", service.getParameters().getOrchestratorToken());
		WebClient.client(connApi).header("X-Auth-Token", service.getParameters().getOrchestratorToken());
		WebClient.client(serviceApi).header("X-Auth-Token", service.getParameters().getOrchestratorToken());
	}

	private boolean breakMyConnection(String processId, String hashPair) {
		String[] hash = hashPair.split(";");
		Process process = getMyProcess(processId);
		List<org.flexiblepower.model.Connection> myConnections = getMyConnections(processId);
		try {
			Service service = serviceApi.getService(process.getServiceId());
			for (Interface i : service.getInterfaces()) {
				for (InterfaceVersion iV : i.getInterfaceVersions()) {
					if (iV.getSendsHash().equals(hash[0]) && iV.getReceivesHash().equals(hash[1])) {
						for (org.flexiblepower.model.Connection connection : myConnections) {
							// if(connection.getEndpoint1().)
						}
					}
				}
			}
		} catch (NotFoundException | AuthorizationException e) {
			LOG.error("There was an error looking up the service for process " + processId);
			return false;
		}

		return true;
	}

	private boolean isConnected(String process1, String process2) {
		List<org.flexiblepower.model.Connection> myConnections = getMyConnections(process1);
		if (myConnections != null) {
			for (org.flexiblepower.model.Connection connection : myConnections) {
				if ((connection.getEndpoint1().getProcessId().toString().equals(process1)
						&& connection.getEndpoint2().getProcessId().toString().equals(process2))
						|| (connection.getEndpoint2().getProcessId().toString().equals(process1)
								&& connection.getEndpoint1().getProcessId().toString().equals(process2))) {
					return true;
				}
			}
		}
		return false;
	}

	private Process getMyProcess(String processId) {
		for (Process process : cemProcessMap.keySet()) {
			if (process.getId().toString().equals(processId)) {
				return process;
			}
		}
		for (Process process : rmProcessMap.keySet()) {
			if (process.getId().toString().equals(processId)) {
				return process;
			}
		}
		for (Process process : obsConProcessMap.keySet()) {
			if (process.getId().toString().equals(processId)) {
				return process;
			}
		}
		for (Process process : obsPubProcessMap.keySet()) {
			if (process.getId().toString().equals(processId)) {
				return process;
			}
		}

		return null;
	}

	private List<org.flexiblepower.model.Connection> getMyConnections(String processId) {
		for (Process process : cemProcessMap.keySet()) {
			if (process.getId().toString().equals(processId)) {
				return cemProcessMap.get(process);
			}
		}
		for (Process process : rmProcessMap.keySet()) {
			if (process.getId().toString().equals(processId)) {
				return rmProcessMap.get(process);
			}
		}
		for (Process process : obsConProcessMap.keySet()) {
			if (process.getId().toString().equals(processId)) {
				return obsConProcessMap.get(process);
			}
		}
		for (Process process : obsPubProcessMap.keySet()) {
			if (process.getId().toString().equals(processId)) {
				return obsPubProcessMap.get(process);
			}
		}

		return null;
	}

	private HashMap<String, String> parseUpdatePost(String body) {
		HashMap<String, String> processMap = new HashMap<>();
		String[] options = body.split("&");
		for (String option : options) {
			String pairs = option.split("=")[1];
			processMap.put(pairs.split("_")[0], pairs.split("_")[1]);
		}

		return processMap;
	}

	private HTTPResponse sendResponse(int id, int status, String response) {
		return HTTPResponse.newBuilder().setId(id).setBody(ByteString.copyFromUtf8(response)).setStatus(status).build();
	}

	private HashMap<Process, List<org.flexiblepower.model.Connection>> getProcessConnectionMap(String hashpair) {
		HashMap<Process, List<org.flexiblepower.model.Connection>> processMap = new HashMap<>();
		List<org.flexiblepower.model.Connection> processConnections;
		List<org.flexiblepower.model.Connection> connectionList;
		JSONObject filter = new JSONObject();
		String encodedFilter = null;
		List<Process> processes = null;
		try {
			filter.put("hashpair", hashpair);
			LOG.debug("hashpair: " + filter.toString());
			encodedFilter = URLEncoder.encode(filter.toString(), "UTF-8");
			LOG.debug("encoded hashpair: " + encodedFilter.toString());
			processes = processApi.listProcesses(encodedFilter);
			for (Process process : processes) {
				filter = new JSONObject();
				filter.put("processId", process.getId());
				encodedFilter = URLEncoder.encode(filter.toString(), "UTF-8");
				connectionList = connApi.listConnections(encodedFilter);

				processConnections = processMap.get(process);
				if (processConnections == null) {
					processMap.put(process, connectionList);
				} else {
					processConnections.addAll(connectionList);
					processMap.put(process, processConnections);
				}
			}
		} catch (UnsupportedEncodingException | AuthorizationException e) {
			e.printStackTrace();
			return null;
		}
		return processMap;
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

	@Override
	public HTTPResponse handle(HTTPRequest message) {
		Method method = message.getMethod();
		String uri = stripURI(message.getUri());
		if (method.equals(Method.GET)) {
			if ("index.html".equals(uri)) {
				cemProcessMap = getProcessConnectionMap(service.getConfig().getCemHashPair());
				rmProcessMap = getProcessConnectionMap(service.getConfig().getRmHashPair());

				String cemPage = new TableModel(cemProcessMap, rmProcessMap,
						"http/dynamic/widgets/ControlAdminFullWidget/sample_cem_table.html").getHTMLTable("radio");
				return sendResponse(message.getId(), 200, cemPage);
			} else if ("observables".equals(uri)) {
				obsConProcessMap = getProcessConnectionMap(service.getConfig().getObsConsumerHashPair());
				obsPubProcessMap = getProcessConnectionMap(service.getConfig().getObsPublisherHashPair());

				String obsPage = new TableModel(obsConProcessMap, obsPubProcessMap,
						"http/dynamic/widgets/ControlAdminFullWidget/sample_obs_table.html").getHTMLTable("checkbox");
				return sendResponse(message.getId(), 200, obsPage);
			}
		} else if (method.equals(Method.POST)) {
			if ("index.html".equals(uri)) {
				HashMap<String, String> processMap = parseUpdatePost(message.getBody());
				for (String cemProcess : processMap.keySet()) {
					if (cemProcess.equals("None")) {

					} else {
						String rmProcess = processMap.get(cemProcess);
						if (!isConnected(cemProcess, rmProcess)) {
							breakMyConnection(rmProcess, service.getConfig().getRmHashPair());
						}
					}
				}
				// TODO
			}
		}
		return sendResponse(message.getId(), 404, method + " to " + uri + " is currently not supported!");
	}

	@Override
	public String getFullWidgetId() {
		return "controladmin";
	}

	@Override
	public String getTitle() {
		return "Control Administration";
	}

	@Override
	public Type getType() {
		return Type.FULL;
	}

}