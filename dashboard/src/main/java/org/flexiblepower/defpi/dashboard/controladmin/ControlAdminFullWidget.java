package org.flexiblepower.defpi.dashboard.controladmin;

import java.util.HashMap;

import org.flexiblepower.defpi.dashboard.Dashboard;
import org.flexiblepower.defpi.dashboard.HttpUtils;
import org.flexiblepower.defpi.dashboard.Widget;
import org.flexiblepower.defpi.dashboard.controladmin.defpi.DefPiConnectionAdmin;
import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPRequest;
import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPRequest.Method;
import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPResponse;
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

	@Override
	public HTTPResponse handle(HTTPRequest message) {
		try {
			Method method = message.getMethod();
			String uri = stripURI(message.getUri());
			if (method.equals(Method.GET)) {
				if ("index.html".equals(uri)) {
					connectionAdmin.refreshData();
					String html = HttpUtils.readTextFile("/dynamic/widgets/ControlAdminFullWidget/index.html");
					html = html.replace("@EFITABLE@", new EfiTableModel(connectionAdmin).generateTable());
					html = html.replace("@OBSTABLE@", new ObsTableModel(connectionAdmin).generateTable());

					return HttpUtils.serveDynamicText(message, HttpUtils.TEXT_HTML, html);
				} else if ("menu.png".equals(uri)) {
					return HttpUtils.serveStaticFile(message, "/dynamic/widgets/ControlAdminFullWidget/menu.png");
				}
			} else if (method.equals(Method.POST)) {
				if ("index.html".equals(uri)) {
					HashMap<String, String> postData = parseUpdatePost(message.getBody());

					LOG.debug("Received the folling POST data: " + postData);

					connectionAdmin.refreshData();
					EfiTableModel efiTableModel = new EfiTableModel(connectionAdmin);
					ObsTableModel obsTableModel = new ObsTableModel(connectionAdmin);
					efiTableModel.handlePost(postData);
					obsTableModel.handlePost(postData);

					String html = HttpUtils.readTextFile("/dynamic/widgets/ControlAdminFullWidget/index.html");
					html = html.replace("@EFITABLE@", efiTableModel.generateTable());
					html = html.replace("@OBSTABLE@", obsTableModel.generateTable());

					return HttpUtils.serveDynamicText(message, HttpUtils.TEXT_HTML, html);
				}
			}
			return HttpUtils.notFound(message);
		} catch (Exception e) {
			LOG.error("Could not generate ControlAdmin response", e);
			return HttpUtils.internalError(message);
		}
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