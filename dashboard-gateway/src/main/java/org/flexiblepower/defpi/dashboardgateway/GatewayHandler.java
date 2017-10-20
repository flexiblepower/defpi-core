package org.flexiblepower.defpi.dashboardgateway;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.core.util.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class GatewayHandler extends AbstractHandler {

	private static final String SESSION_COOKIE_NAME = "EFPISESSION";

	private DashboardGateway main;
	private Map<String, String> sessions = new ConcurrentHashMap<>();

	private CemSelector cemSelector = new CemSelector();

	public GatewayHandler(DashboardGateway main) {
		this.main = main;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		try {
			System.out.println(request.getMethod() + " " + request.getRequestURI());
			String userEmail = getUserEmail(request);

			if (userEmail == null) {
				handleNotAuthenticated(target, baseRequest, request, response);
			} else {
				handleAuthenticated(userEmail, target, baseRequest, request, response);
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	private void handleNotAuthenticated(String target, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		if (request.getRequestURI().equals("/") && request.getMethod().equals("GET")) {
			// server login page
			response.setStatus(200);
			response.setHeader("content-type", "text/html");
			response.getWriter().print(
					"<htm><body><form method=post><input type=text name=username /><input type=password name=password /><input type=submit name=submit /></form></body></html>");
			response.getWriter().close();
		} else if (request.getRequestURI().equals("/") && request.getMethod().equals("POST")) {
			// Try to login
			Map<String, String> params = parseParams(request);
			if (params.containsKey("username") && params.containsKey("password")
					&& validCredentials(params.get("username"), params.get("password"))) {
				// Valid!

				// Set cookie
				String sessionKey = UUID.randomUUID().toString();
				sessions.put(sessionKey, params.get("username"));
				Cookie cookie = new Cookie(SESSION_COOKIE_NAME, sessionKey);
				cookie.setMaxAge(-1);
				response.addCookie(cookie);

				// Redirect
				response.setStatus(301);
				response.setHeader("Location", "/");
				response.getWriter().close();
			} else {
				// Invalid!
				response.setStatus(200);
				response.setHeader("content-type", "text/html");
				response.getWriter().print("Failed to login");
				response.getWriter().close();
			}
		} else {
			// Redirect to the login page
			response.setStatus(301);
			response.setHeader("Location", "/");
			response.getWriter().close();
		}
	}

	private boolean validCredentials(String string, String string2) {
		// TODO Auto-generated method stub
		return true;
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

	private void handleAuthenticated(String userEmail, String target, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		if (request.getRequestURI().equals("/logout")) {
			// Remove session
			Cookie cookie = new Cookie(SESSION_COOKIE_NAME, "");
			cookie.setMaxAge(0);
			response.addCookie(cookie);

			// Redirect
			response.setStatus(301);
			response.setHeader("Location", "/");
			response.getWriter().close();
		} else {
			cemSelector.handleAuthenticated(userEmail, target, baseRequest, request, response);
			// Dashboard_httpConnectionHandlerImpl handler =
			// main.getHandlerForUserEmail(userEmail);
			// if (handler == null) {
			// response.setHeader("content-type", "text/html");
			// response.getWriter().print("<h1>No dashboard found</h1><p><a
			// href=\"/logout\">Logout</a>");
			// response.getWriter().close();
			// } else {
			// handler.handle(target, baseRequest, request, response);
			// }
		}
	}

	private String getUserEmail(HttpServletRequest request) {
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

}
