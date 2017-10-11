package org.flexiblepower.defpi.dashboardgateway;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class GatewayHandler extends AbstractHandler {

	private DashboardGateway main;

	public GatewayHandler(DashboardGateway main) {
		this.main = main;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		String userEmail = getUserEmail(request);

		if (userEmail == null) {
			serveLogin(target, baseRequest, request, response);
		} else {

		}

	}

	private void serveLogin(String target, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) {
		// TODO Auto-generated method stub
	}

	private void serveNoDashboardFound(String target, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) {
		// TODO Auto-generated method stub
	}

	private String getUserEmail(HttpServletRequest request) {
		String header = request.getHeader("Authorization");
		if (header == null) {
			// No login provided
		}
		// TODO Check login credentials with orchestrator. If incorrect, return
		// null;
		return null;
	}

}
