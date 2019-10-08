package org.flexiblepower.defpi.dashboardgateway;

/*-
 * #%L
 * dEF-Pi dashboard gateway
 * %%
 * Copyright (C) 2017 - 2018 Flexible Power Alliance Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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

    private final DashboardGateway main;
    private final Map<String, String> sessions = new ConcurrentHashMap<>();

    public GatewayHandler(final DashboardGateway main) {
        this.main = main;
    }

    @Override
    public void handle(final String target,
            final Request baseRequest,
            final HttpServletRequest request,
            final HttpServletResponse response) throws IOException,
            ServletException {
        DashboardGateway.LOG.debug(request.getMethod() + " " + request.getRequestURI());
        final String username = this.getUsername(request);

        if (username == null) {
            // this.handleNotAuthenticated(target, baseRequest, request, response);
            this.handleNotAuthenticated(request, response);
        } else {
            this.handleAuthenticated(username, target, baseRequest, request, response);
        }
    }

    private void handleNotAuthenticated(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        if (request.getRequestURI().equals("/") && request.getMethod().equals("GET")) {
            // server login page
            response.setStatus(200);
            response.setHeader("content-type", "text/html");
            response.setHeader(GatewayHandler.NO_CACHE_KEY, GatewayHandler.NO_CACHE_VALUE);
            String html = new String(GatewayHandler.readFile("/dynamic/login.html"));
            html = html.replace("$error$", "");

            response.getWriter().print(html);
            response.getWriter().close();
        } else if (request.getRequestURI().equals("/") && request.getMethod().equals("POST")) {
            // Try to login
            final Map<String, String> params = GatewayHandler.parseParams(request);
            if (params.containsKey("username") && params.containsKey("password")
                    && this.main.validCredentials(params.get("username"), params.get("password"))) {
                // Valid!

                // Set cookie
                final String sessionKey = UUID.randomUUID().toString();
                this.sessions.put(sessionKey, params.get("username"));
                final Cookie cookie = new Cookie(GatewayHandler.SESSION_COOKIE_NAME, sessionKey);
                cookie.setMaxAge(-1);
                response.addCookie(cookie);

                // Redirect
                response.setStatus(302);
                response.setHeader("Location", "/");
                response.setHeader(GatewayHandler.NO_CACHE_KEY, GatewayHandler.NO_CACHE_VALUE);
                response.getWriter().close();
            } else {
                // Invalid!
                // server login page
                response.setStatus(200);
                response.setHeader("content-type", "text/html");
                response.setHeader(GatewayHandler.NO_CACHE_KEY, GatewayHandler.NO_CACHE_VALUE);
                String html = new String(GatewayHandler.readFile("/dynamic/login.html"));
                html = html.replace("$error$", "<p class=\"error\">Invalid credentials</p>");

                response.getWriter().print(html);
                response.getWriter().close();
            }
        } else if (new File("/static/" + request.getRequestURI()).exists()) {
            // Serve static content
            response.setStatus(200);
            response.setHeader("content-type", GatewayHandler.getContentType(request.getRequestURI()));
            response.setHeader(GatewayHandler.NO_CACHE_KEY, GatewayHandler.NO_CACHE_VALUE);
            GatewayHandler.writeStaticFile("/static/" + request.getRequestURI(), response.getOutputStream());
            response.getOutputStream().close();
        } else {
            // Redirect to the login page
            response.setStatus(302);
            response.setHeader("Location", "/");
            response.setHeader(GatewayHandler.NO_CACHE_KEY, GatewayHandler.NO_CACHE_VALUE);
            response.getWriter().close();
        }
    }

    public static Map<String, String> parseParams(final HttpServletRequest request) throws IOException {
        final String content = IOUtils.toString(request.getReader());
        final Map<String, String> params = new HashMap<>();
        for (final String line : content.split("&")) {
            if (line.contains("=")) {
                final String[] split = line.split("=");
                params.put(split[0], split[1]);
            }
        }
        return params;
    }

    private void handleAuthenticated(final String username,
            final String target,
            final Request baseRequest,
            final HttpServletRequest request,
            final HttpServletResponse response) throws IOException {
        if (request.getRequestURI().equals("/logout")) {
            DashboardGateway.LOG.debug("User with username " + username + " logged out");

            // Remove session
            final Cookie cookie = new Cookie(GatewayHandler.SESSION_COOKIE_NAME, "");
            cookie.setMaxAge(0);
            response.addCookie(cookie);

            // Remove session
            final Iterator<Entry<String, String>> it = this.sessions.entrySet().iterator();
            while (it.hasNext()) {
                if (it.next().getValue().equals(username)) {
                    it.remove();
                }
            }

            // Redirect
            response.setStatus(302);
            response.setHeader("Location", "/");
            response.setHeader(GatewayHandler.NO_CACHE_KEY, GatewayHandler.NO_CACHE_VALUE);
            response.getWriter().close();
        } else {
            final Dashboard_httpConnectionHandlerImpl handler = this.main.getHandlerForUsername(username);
            if (handler == null) {
                DashboardGateway.LOG
                        .warn("User " + username + " logged in, but there is no dashboard found for this user");
                response.setHeader("content-type", "text/html");
                response.setHeader(GatewayHandler.NO_CACHE_KEY, GatewayHandler.NO_CACHE_VALUE);
                response.getWriter().print(
                        "<html><body><h1>No dashboard found</h1><p><a href=\"/logout\">Logout</a></p></body></html>");
                response.getWriter().close();
            } else {
                handler.handle(target, baseRequest, request, response);
            }
        }
    }

    private String getUsername(final HttpServletRequest request) {
        final Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (final Cookie cookie : cookies) {
                if (cookie.getName().equals(GatewayHandler.SESSION_COOKIE_NAME)) {
                    return this.sessions.get(cookie.getValue());
                }
            }
        }
        return null;
    }

    private static byte[] readFile(final String filename) {
        try (FileInputStream in = new FileInputStream(new File(filename));) {

            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            return out.toByteArray();
        } catch (final Exception e) {
            return null;
        }
    }

    private static boolean writeStaticFile(final String filename, final OutputStream out) {
        try (FileInputStream in = new FileInputStream(new File(filename));) {
            final byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            return true;
        } catch (final Exception e) {
            return false;
        }
    }

    private static String getContentType(final String filename) {
        final String[] uri = filename.split("\\.");

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
