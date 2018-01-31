package org.flexiblepower.defpi.dashboard.controladmin;

import java.util.HashMap;

import org.flexiblepower.defpi.dashboard.Dashboard;
import org.flexiblepower.defpi.dashboard.HttpTask;
import org.flexiblepower.defpi.dashboard.HttpUtils;
import org.flexiblepower.defpi.dashboard.Widget;
import org.flexiblepower.defpi.dashboard.controladmin.defpi.DefPiConnectionAdmin;
import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPRequest.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	public void handle(HttpTask httpTask) {
		try {
			Method method = httpTask.getRequest().getMethod();
			String uri = stripURI(httpTask.getUri());
			if (method.equals(Method.GET)) {
				if ("index.html".equals(uri)) {
					connectionAdmin.refreshData();
					String html = HttpUtils.readTextFile("/dynamic/widgets/ControlAdminFullWidget/index.html");
					html = html.replace("@EFITABLE@", new EfiTableModel(connectionAdmin).generateTable());
					html = html.replace("@OBSTABLE@", new ObsTableModel(connectionAdmin).generateTable());

					HttpUtils.serveDynamicText(httpTask, HttpUtils.TEXT_HTML, html);
					return;
				} else if ("menu.png".equals(uri)) {
					HttpUtils.serveStaticFile(httpTask, "/dynamic/widgets/ControlAdminFullWidget/menu.png");
					return;
				}
			} else if (method.equals(Method.POST)) {
				if ("index.html".equals(uri)) {
					HashMap<String, String> postData = parseUpdatePost(httpTask.getRequest().getBody());

					LOG.debug("Received the folling POST data: " + postData);

					connectionAdmin.refreshData();
					EfiTableModel efiTableModel = new EfiTableModel(connectionAdmin);
					ObsTableModel obsTableModel = new ObsTableModel(connectionAdmin);
					efiTableModel.handlePost(postData);
					obsTableModel.handlePost(postData);

					String html = HttpUtils.readTextFile("/dynamic/widgets/ControlAdminFullWidget/index.html");
					html = html.replace("@EFITABLE@", efiTableModel.generateTable());
					html = html.replace("@OBSTABLE@", obsTableModel.generateTable());

					HttpUtils.serveDynamicText(httpTask, HttpUtils.TEXT_HTML, html);
					return;
				}
			}
			HttpUtils.notFound(httpTask);
		} catch (Exception e) {
			LOG.error("Could not generate ControlAdmin response", e);
			HttpUtils.internalError(httpTask);
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