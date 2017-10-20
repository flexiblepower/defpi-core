package org.flexiblepower.defpi.dashboardgateway;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;

public class CemSelector {

	private Map<String, String> cems = new HashMap<>();
	private Map<String, String> rms = new HashMap<>();

	public CemSelector() {
		cems.put("pm", "PowerMatcher");
		rms.put("2-PVMETERSWITCH-1", "none");
		rms.put("2-PVMETER-1", "none");
	}

	public void handleAuthenticated(String userEmail, String target, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		if (request.getMethod().equals("POST")) {
			Map<String, String> params = GatewayHandler.parseParams(request);
			for (Entry<String, String> e : rms.entrySet()) {
				if (params.containsKey(e.getKey())) {
					rms.put(e.getKey(), params.get(e.getKey()));
				}
			}
		}

		StringBuilder html = new StringBuilder();

		List<String> cemIds = new ArrayList<>(cems.keySet());

		// Header
		html.append("<htm><body><form method=post><table><tr><th></th>");
		for (String cemId : cemIds) {
			html.append("<th>");
			html.append(cems.get(cemId));
			html.append("</th>");
		}
		html.append("<th>None</th></tr>");

		// Content
		for (String rm : rms.keySet()) {
			html.append("<tr><td>");
			html.append(rm);
			html.append("</td>");
			for (String cemId : cemIds) {
				html.append("<td>");
				html.append("<input type=\"radio\" name=\"" + rm + "\" value=\"" + cemId + "\"");
				if (rms.get(rm).equals(cemId)) {
					html.append(" checked");
				}
				html.append(">");
				html.append("</td>");
			}
			html.append("<td>");
			html.append("<input type=\"radio\" name=\"" + rm + "\" value=\"none\"");
			if (rms.get(rm).equals("none")) {
				html.append(" checked");
			}
			html.append(">");
			html.append("</td>");
			html.append("</tr>");
		}

		html.append("</table><input type=submit name=submit /></form></body></html>");

		response.setHeader("content-type", "text/html");
		response.setStatus(200);
		response.getWriter().append(html.toString());
		response.getWriter().close();
	}

}
