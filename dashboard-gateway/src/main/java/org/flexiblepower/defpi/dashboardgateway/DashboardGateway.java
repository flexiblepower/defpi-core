/**
 * File DashboardGateway.java
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

package org.flexiblepower.defpi.dashboardgateway;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Generated;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Server;
import org.flexiblepower.defpi.dashboardgateway.dashboard.http.Dashboard_httpConnectionHandlerImpl;
import org.flexiblepower.service.DefPiParameters;
import org.flexiblepower.service.Service;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DashboardGateway provides an implementation of the Dashboard Gateway service
 *
 * File generated by org.flexiblepower.create-service-maven-plugin. NOTE: This
 * file is generated as a stub, and has to be implemented by the user.
 * Re-running the codegen plugin will not change the contents of this file.
 * Template by FAN, 2017
 * 
 */
@Generated(value = "org.flexiblepower.plugin.servicegen", date = "Oct 9, 2017 8:45:27 PM")
public class DashboardGateway implements Service<Void> {

	public static final Logger LOG = LoggerFactory.getLogger(DashboardGateway.class);

	private Server server;
	private Map<String, Dashboard_httpConnectionHandlerImpl> dashboardConnections = Collections
			.synchronizedMap(new HashMap<>());

	private ServerThread serverThread;

	private DefPiParameters params;

	private class ServerThread extends Thread {
		@Override
		public void run() {
			try {
				server.start();
				server.join();
			} catch (Exception e) {
				LOG.error("Error while running Jetty HTTP Server", e);
			}
		}
	}

	@Override
	public void resumeFrom(Serializable state) {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(Void config, DefPiParameters params) {
		try {
			this.params = params;
			server = new Server(8080);
			server.setHandler(new GatewayHandler(this));
			serverThread = new ServerThread();
			serverThread.start();
		} catch (Exception e) {
			LOG.error("Could not start DashboardGateway HTTP server", e);
		}
	}

	@Override
	public void modify(Void config) {
		// TODO Auto-generated method stub
	}

	@Override
	public Serializable suspend() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void terminate() {
		try {
			server.stop();
		} catch (Exception e) {
			LOG.error("Could not stop DashboardGateway HTTP server", e);
		}
	}

	public void addDashboardConnection(Dashboard_httpConnectionHandlerImpl connection) {
		this.dashboardConnections.put(connection.getUsername(), connection);
	}

	public void removeDashboardConnection(Dashboard_httpConnectionHandlerImpl connection) {
		this.dashboardConnections.remove(connection.getUsername());
	}

	public Dashboard_httpConnectionHandlerImpl getHandlerForUsername(String username) {
		return this.dashboardConnections.get(username);
	}

	public boolean validCredentials(String username, String password) {
		try {
			URL orch = new URL("http://" + params.getOrchestratorHost() + ":" + params.getOrchestratorPort()
					+ "/user/by_username/" + URLEncoder.encode(username, "UTF-8"));
			HttpURLConnection con = (HttpURLConnection) orch.openConnection();
			con.setRequestProperty("Authorization",
					"Basic " + new String(Base64.getEncoder().encode((username + ":" + password).getBytes())));
			con.setRequestMethod("GET");
			int code = con.getResponseCode();
			boolean success = code == 200;
			if (success) {
				LOG.info("Attempted login for user " + username + " was successful");
			} else {
				LOG.info("Attempted login for user " + username + " failed");

			}
			return success;
		} catch (IOException e) {
			return false;
		}
	}

	public String getUsernameForProcessId(String processId) {
		try {
			// Retrieve the userId
			URL orch = new URL("http://" + params.getOrchestratorHost() + ":" + params.getOrchestratorPort()
					+ "/process/" + URLEncoder.encode(processId, "UTF-8"));
			HttpURLConnection con = (HttpURLConnection) orch.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("X-Auth-Token", params.getOrchestratorToken());
			int code = con.getResponseCode();
			LOG.debug("GET to " + orch.toString() + " returned : " + code);

			String responseBody = IOUtils.toString(new InputStreamReader(con.getInputStream()));

			JSONObject obj = new JSONObject(responseBody);
			String userId = obj.getString("userId");

			if (userId == null) {
				LOG.error("Got an invalid response from " + orch.toString() + ", received " + responseBody);
				return null;
			}

			// Retrieve the username
			orch = new URL("http://" + params.getOrchestratorHost() + ":" + params.getOrchestratorPort() + "/user/"
					+ URLEncoder.encode(userId, "UTF-8"));
			con = (HttpURLConnection) orch.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("X-Auth-Token", params.getOrchestratorToken());
			code = con.getResponseCode();
			LOG.debug("GET to " + orch.toString() + " returned : " + code);

			responseBody = IOUtils.toString(new InputStreamReader(con.getInputStream()));

			obj = new JSONObject(responseBody);
			return obj.getString("username");

		} catch (IOException e) {
			LOG.error("Could not determine the owner of process " + processId, e);
			return null;
		}
	}

}
