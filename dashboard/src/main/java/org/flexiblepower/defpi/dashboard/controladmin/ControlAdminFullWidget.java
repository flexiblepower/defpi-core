package org.flexiblepower.defpi.dashboard.controladmin;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import org.flexiblepower.defpi.dashboard.Dashboard;
import org.flexiblepower.defpi.dashboard.HttpUtils;
import org.flexiblepower.defpi.dashboard.Widget;
import org.flexiblepower.defpi.dashboard.controladmin.defpi.CemProcess;
import org.flexiblepower.defpi.dashboard.controladmin.defpi.DefPiConnectionAdmin;
import org.flexiblepower.defpi.dashboard.controladmin.defpi.RmProcess;
import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPRequest;
import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPRequest.Method;
import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPResponse;
import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.ConnectionException;
import org.flexiblepower.exceptions.InvalidObjectIdException;
import org.flexiblepower.exceptions.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author subramaniana
 */
public class ControlAdminFullWidget implements Widget {

	public static final Logger LOG = LoggerFactory.getLogger(ControlAdminFullWidget.class);
	private final Dashboard service;
	private DefPiConnectionAdmin connectionAdmin;

	/**
	 * Auto-generated constructor for the ConnectionHandlers of the provided service
	 *
	 * @param service
	 *            The service for which to handle the connections
	 */
	public ControlAdminFullWidget(Dashboard service) {
		this.service = service;
		this.connectionAdmin = new DefPiConnectionAdmin(service.getParameters());
	}
	//
	// private boolean breakMyConnection(String processId, String hashPair) {
	// String[] hash = hashPair.split(";");
	// Process process = getMyProcess(processId);
	// List<org.flexiblepower.model.Connection> myConnections =
	// getMyConnections(processId);
	// try {
	// Service service = serviceApi.getService(process.getServiceId());
	// for (Interface i : service.getInterfaces()) {
	// for (InterfaceVersion iV : i.getInterfaceVersions()) {
	// if (iV.getSendsHash().equals(hash[0]) &&
	// iV.getReceivesHash().equals(hash[1])) {
	// for (org.flexiblepower.model.Connection connection : myConnections) {
	// // if(connection.getEndpoint1().)
	// }
	// }
	// }
	// }
	// } catch (NotFoundException | AuthorizationException e) {
	// LOG.error("There was an error looking up the service for process " +
	// processId);
	// return false;
	// }
	//
	// return true;
	// }
	//
	// private boolean isConnected(String process1, String process2) {
	// List<org.flexiblepower.model.Connection> myConnections =
	// getMyConnections(process1);
	// if (myConnections != null) {
	// for (org.flexiblepower.model.Connection connection : myConnections) {
	// if ((connection.getEndpoint1().getProcessId().toString().equals(process1)
	// && connection.getEndpoint2().getProcessId().toString().equals(process2))
	// || (connection.getEndpoint2().getProcessId().toString().equals(process1)
	// && connection.getEndpoint1().getProcessId().toString().equals(process2))) {
	// return true;
	// }
	// }
	// }
	// return false;
	// }
	//
	// private Process getMyProcess(String processId) {
	// for (Process process : cemProcessMap.keySet()) {
	// if (process.getId().toString().equals(processId)) {
	// return process;
	// }
	// }
	// for (Process process : rmProcessMap.keySet()) {
	// if (process.getId().toString().equals(processId)) {
	// return process;
	// }
	// }
	// for (Process process : obsConProcessMap.keySet()) {
	// if (process.getId().toString().equals(processId)) {
	// return process;
	// }
	// }
	// for (Process process : obsPubProcessMap.keySet()) {
	// if (process.getId().toString().equals(processId)) {
	// return process;
	// }
	// }
	//
	// return null;
	// }
	//
	// private List<org.flexiblepower.model.Connection> getMyConnections(String
	// processId) {
	// for (Process process : cemProcessMap.keySet()) {
	// if (process.getId().toString().equals(processId)) {
	// return cemProcessMap.get(process);
	// }
	// }
	// for (Process process : rmProcessMap.keySet()) {
	// if (process.getId().toString().equals(processId)) {
	// return rmProcessMap.get(process);
	// }
	// }
	// for (Process process : obsConProcessMap.keySet()) {
	// if (process.getId().toString().equals(processId)) {
	// return obsConProcessMap.get(process);
	// }
	// }
	// for (Process process : obsPubProcessMap.keySet()) {
	// if (process.getId().toString().equals(processId)) {
	// return obsPubProcessMap.get(process);
	// }
	// }
	//
	// return null;
	// }

	private HashMap<String, String> parseUpdatePost(String body) {
		LOG.debug("body to parse: " + body);
		HashMap<String, String> map = new HashMap<>();
		String[] options = body.split("&");
		for (String option : options) {
			if (option.contains("=")) {
				String[] pairs = option.split("=");
				map.put(pairs[0], pairs[1]);
			}
		}
		LOG.debug("result: " + map);
		return map;
	}
	//
	// private HTTPResponse sendResponse(int id, int status, String response) {
	// return
	// HTTPResponse.newBuilder().setId(id).setBody(ByteString.copyFromUtf8(response)).setStatus(status).build();
	// }

	// private HashMap<Process, List<org.flexiblepower.model.Connection>>
	// getProcessConnectionMap(String hashpair) {
	// HashMap<Process, List<org.flexiblepower.model.Connection>> processMap = new
	// HashMap<>();
	// List<org.flexiblepower.model.Connection> processConnections;
	// List<org.flexiblepower.model.Connection> connectionList;
	// JSONObject filter = new JSONObject();
	// String encodedFilter = null;
	// List<Process> processes = null;
	// try {
	// filter.put("hashpair", hashpair);
	// LOG.debug("hashpair: " + filter.toString());
	// encodedFilter = URLEncoder.encode(filter.toString(), "UTF-8");
	// LOG.debug("encoded hashpair: " + encodedFilter.toString());
	// processes = processApi.listProcesses(encodedFilter);
	// for (Process process : processes) {
	// filter = new JSONObject();
	// filter.put("processId", process.getId());
	// encodedFilter = URLEncoder.encode(filter.toString(), "UTF-8");
	// connectionList = connApi.listConnections(encodedFilter);
	//
	// processConnections = processMap.get(process);
	// if (processConnections == null) {
	// processMap.put(process, connectionList);
	// } else {
	// processConnections.addAll(connectionList);
	// processMap.put(process, processConnections);
	// }
	// }
	// } catch (UnsupportedEncodingException | AuthorizationException e) {
	// e.printStackTrace();
	// return null;
	// }
	// return processMap;
	// }

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

	private boolean efiIsConnected(DefPiConnectionAdmin connectionAdmin) {
		for (CemProcess cem : connectionAdmin.listCems()) {
			if (cem.getRmProcessIds().isEmpty()) {
				return false;
			} else {
				return true;
			}
		}
		// Als er geen CEMs zijn gaan we er maar vanuit dat er wel EFI connecties zouden
		// moeten zijn
		return true;
	}

	private void connectAllEfiSessions() {
		// For now we assume there is 0 or 1 CEM
		if (connectionAdmin.listCems().isEmpty()) {
			return;
		}
		try {
			CemProcess cem = connectionAdmin.listCems().iterator().next();
			for (RmProcess rm : connectionAdmin.listRms()) {
				connectionAdmin.connect(cem, rm);
			}
		} catch (ConnectionException | AuthorizationException | NotFoundException | IOException e) {
			LOG.error("Could not create EFI connection", e);
		}
	}

	private void disconnectAllEfiSessions() {
		// For now we assumer there is 0 or 1 CEM
		if (connectionAdmin.listCems().isEmpty()) {
			return;
		}
		try {
			CemProcess cem = connectionAdmin.listCems().iterator().next();
			for (RmProcess rm : connectionAdmin.listRms()) {
				connectionAdmin.disconnect(cem, rm);
			}
		} catch (UnsupportedEncodingException | InvalidObjectIdException | AuthorizationException
				| NotFoundException e) {
			LOG.error("Could not remove EFI connection", e);
		}
	}

	@Override
	public HTTPResponse handle(HTTPRequest message) {
		try {
			Method method = message.getMethod();
			String uri = stripURI(message.getUri());
			if (method.equals(Method.GET)) {
				if ("index.html".equals(uri)) {
					return servePackageOptions(message);
				} else if ("menu.png".equals(uri)) {
					return HttpUtils.serveStaticFile(message, "/dynamic/widgets/ControlAdminFullWidget/menu.png");
				}
			} else if (method.equals(Method.POST)) {
				if ("index.html".equals(uri)) {
					HashMap<String, String> postData = parseUpdatePost(message.getBody());

					LOG.debug("Received the folling POST data: " + postData);

					if (postData.containsKey("option")) {
						String value = postData.get("option");
						if ("sympower".equals(value)) {
							LOG.info("Creating EFI sessions");
							connectAllEfiSessions();
						} else if ("alleenmeten".equals(value)) {
							LOG.info("Removing EFI sessions");
							disconnectAllEfiSessions();
						}
					}

					connectionAdmin.refreshData();
					return servePackageOptions(message);
				}
			}
			return HttpUtils.notFound(message);
		} catch (Exception e) {
			LOG.error("Could not generate ControlAdmin response", e);
			return HttpUtils.internalError(message);
		}
	}

	private HTTPResponse servePackageOptions(HTTPRequest message)
			throws UnsupportedEncodingException, AuthorizationException, IOException {
		connectionAdmin.refreshData();
		String html = HttpUtils.readTextFile("/dynamic/widgets/ControlAdminFullWidget/index.html");

		if (efiIsConnected(connectionAdmin)) {
			html = html.replace("@SYMPOWER_STYLE@", "optionactive");
			html = html.replace("@ALLEENMETEN_STYLE@", "optionnotactive");
		} else {
			html = html.replace("@SYMPOWER_STYLE@", "optionnotactive");
			html = html.replace("@ALLEENMETEN_STYLE@", "optionactive");
		}

		return HttpUtils.serveDynamicText(message, HttpUtils.TEXT_HTML, html);
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