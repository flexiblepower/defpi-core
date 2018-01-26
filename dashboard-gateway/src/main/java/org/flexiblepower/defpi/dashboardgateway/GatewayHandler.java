/**
 * File GatewayHandler.java
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.flexiblepower.defpi.dashboardgateway.dashboard.http.Dashboard_httpConnectionHandlerImpl;

public class GatewayHandler extends AbstractHandler {

	private static final String SESSION_COOKIE_NAME = "EFPISESSION";
	public static final String NO_CACHE_KEY = "Cache-Control";
	public static final String NO_CACHE_VALUE = "no-cache, no-store, must-revalidate";

	private DashboardGateway main;
	private Map<String, String> sessions = new ConcurrentHashMap<>();

	public GatewayHandler(DashboardGateway main) {
		this.main = main;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		DashboardGateway.LOG.debug(request.getMethod() + " " + request.getRequestURI());
		String username = getUsername(request);

		if (username == null) {
			handleNotAuthenticated(target, baseRequest, request, response);
		} else {
			handleAuthenticated(username, target, baseRequest, request, response);
		}
	}

	private void handleNotAuthenticated(String target, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		if (request.getRequestURI().equals("/") && request.getMethod().equals("GET")) {
			// server login page
			response.setStatus(200);
			response.setHeader("content-type", "text/html");
			response.setHeader(NO_CACHE_KEY, NO_CACHE_VALUE);
			String html = new String(readFile("/dynamic/login.html"));
			html = html.replace("$error$", "");

			response.getWriter().print(html);
			response.getWriter().close();
		} else if (request.getRequestURI().equals("/") && request.getMethod().equals("POST")) {
			// Try to login
			Map<String, String> params = parseParams(request);
			if (params.containsKey("username") && params.containsKey("password")
					&& main.validCredentials(params.get("username"), params.get("password"))) {
				// Valid!

				// Set cookie
				String sessionKey = UUID.randomUUID().toString();
				sessions.put(sessionKey, params.get("username"));
				Cookie cookie = new Cookie(SESSION_COOKIE_NAME, sessionKey);
				cookie.setMaxAge(-1);
				response.addCookie(cookie);

				// Redirect
				response.setStatus(302);
				response.setHeader("Location", "/");
				response.setHeader(NO_CACHE_KEY, NO_CACHE_VALUE);
				response.getWriter().close();
			} else {
				// Invalid!
				// server login page
				response.setStatus(200);
				response.setHeader("content-type", "text/html");
				response.setHeader(NO_CACHE_KEY, NO_CACHE_VALUE);
				String html = new String(readFile("/dynamic/login.html"));
				html = html.replace("$error$", "<p class=\"error\">Invalid credentials</p>");

				response.getWriter().print(html);
				response.getWriter().close();
			}
		} else if (new File("/static/" + request.getRequestURI()).exists()) {
			// Serve static content
			response.setStatus(200);
			response.setHeader("content-type", getContentType(request.getRequestURI()));
			response.setHeader(NO_CACHE_KEY, NO_CACHE_VALUE);
			writeStaticFile("/static/" + request.getRequestURI(), response.getOutputStream());
			response.getOutputStream().close();
		} else {
			// Redirect to the login page
			response.setStatus(302);
			response.setHeader("Location", "/");
			response.setHeader(NO_CACHE_KEY, NO_CACHE_VALUE);
			response.getWriter().close();
		}
	}

	public static Map<String, String> parseParams(HttpServletRequest request) throws IOException {
		String content = IOUtils.toString(request.getReader());
		Map<String, String> params = new HashMap<>();
		for (String line : content.split("&")) {
			if (line.contains("=")) {
				String[] split = line.split("=");
				params.put(split[0], split[1]);
			}
		}
		return params;
	}

	private void handleAuthenticated(String username, String target, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		if (request.getRequestURI().equals("/logout")) {
			DashboardGateway.LOG.debug("User with username " + username + " logged out");

			// Remove session
			Cookie cookie = new Cookie(SESSION_COOKIE_NAME, "");
			cookie.setMaxAge(0);
			response.addCookie(cookie);

			// Remove session
			Iterator<Entry<String, String>> it = sessions.entrySet().iterator();
			while (it.hasNext()) {
				if (it.next().getValue().equals(username)) {
					it.remove();
				}
			}

			// Redirect
			response.setStatus(302);
			response.setHeader("Location", "/");
			response.setHeader(NO_CACHE_KEY, NO_CACHE_VALUE);
			response.getWriter().close();
		} else {
			Dashboard_httpConnectionHandlerImpl handler = main.getHandlerForUsername(username);
			if (handler == null) {
				DashboardGateway.LOG
						.warn("User " + username + " logged in, but there is no dashboard found for this user");
				response.setHeader("content-type", "text/html");
				response.setHeader(NO_CACHE_KEY, NO_CACHE_VALUE);
				response.getWriter().print(
						"<html><body><h1>No dashboard found</h1><p><a href=\"/logout\">Logout</a></p></body></html>");
				response.getWriter().close();
			} else {
				handler.handle(target, baseRequest, request, response);
			}
		}
	}

	private String getUsername(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals(SESSION_COOKIE_NAME)) {
					return sessions.get(cookie.getValue());
				}
			}
		}
		return null;
	}

	private static byte[] readFile(String filename) {
		try (FileInputStream in = new FileInputStream(new File(filename));) {

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int len;
			while ((len = in.read(buffer)) > 0) {
				out.write(buffer, 0, len);
			}
			return out.toByteArray();
		} catch (Exception e) {
			return null;
		}
	}

	private static boolean writeStaticFile(String filename, OutputStream out) {
		try (FileInputStream in = new FileInputStream(new File(filename));) {
			byte[] buffer = new byte[1024];
			int len;
			while ((len = in.read(buffer)) > 0) {
				out.write(buffer, 0, len);
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private static String getContentType(String filename) {
		String[] uri = filename.split("\\.");

		String contentType;
		switch (uri[uri.length - 1].toLowerCase()) {
		case "html":
			contentType = "text/html";
			break;
		case "png":
			contentType = "image/png";
			break;
		case "jpg":
			contentType = "image/jpeg";
			break;
		case "jpeg":
			contentType = "image/jpeg";
			break;
		case "gif":
			contentType = "image/gif";
			break;
		case "js":
			contentType = "application/javascript";
			break;
		case "css":
			contentType = "text/css";
			break;
		default:
			contentType = "text/plain";
		}
		return contentType;
	}

}
