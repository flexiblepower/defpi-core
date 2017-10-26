package org.flexiblepower.defpi.dashboardgateway.dashboard.http;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Generated;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.flexiblepower.defpi.dashboardgateway.DashboardGateway;
import org.flexiblepower.defpi.dashboardgateway.dashboard.http.proto.Dashboard_httpProto.HTTPRequest;
import org.flexiblepower.defpi.dashboardgateway.dashboard.http.proto.Dashboard_httpProto.HTTPRequest.Builder;
import org.flexiblepower.defpi.dashboardgateway.dashboard.http.proto.Dashboard_httpProto.HTTPRequest.Method;
import org.flexiblepower.defpi.dashboardgateway.dashboard.http.proto.Dashboard_httpProto.HTTPResponse;
import org.flexiblepower.service.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

/**
 * Dashboard_httpConnectionHandlerImpl
 *
 * File generated by org.flexiblepower.create-service-maven-plugin. NOTE: This
 * file is generated as a stub, and has to be implemented by the user.
 * Re-running the codegen plugin will not change the contents of this file.
 * Template by TNO, 2017
 * 
 * @author wilco
 */
@Generated(value = "org.flexiblepower.plugin.servicegen", date = "Oct 9, 2017 8:45:27 PM")
public class Dashboard_httpConnectionHandlerImpl implements Dashboard_httpConnectionHandler {

	public static final Logger LOG = LoggerFactory.getLogger(Dashboard_httpConnectionHandlerImpl.class);

	private final Connection connection;
	private final DashboardGateway service;
	private final AtomicInteger requestIdGenerator = new AtomicInteger(0);
	private final Map<Integer, CompletableFuture<HTTPResponse>> responseList = new ConcurrentHashMap<>();
	private String username = null;

	/**
	 * Auto-generated constructor for the ConnectionHandlers of the provided service
	 *
	 * @param service
	 *            The service for which to handle the connections
	 */
	public Dashboard_httpConnectionHandlerImpl(Connection connection, DashboardGateway service) {
		this.connection = connection;
		this.service = service;
		service.addDashboardConnection(this);
	}

	@Override
	public void handleHTTPResponseMessage(HTTPResponse message) {
		CompletableFuture<HTTPResponse> completableFuture = this.responseList.get(message.getId());
		if (completableFuture == null) {
			LOG.error("Received HTTPResponse for unknown request id: " + message.getId());
		} else {
			completableFuture.complete(message);
		}
	}

	@Override
	public void onSuspend() {
		// TODO Auto-generated method stub

	}

	@Override
	public void resumeAfterSuspend() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onInterrupt() {
		// TODO Auto-generated method stub

	}

	@Override
	public void resumeAfterInterrupt() {
		// TODO Auto-generated method stub
	}

	@Override
	public void terminated() {
		service.removeDashboardConnection(this);
	}

	public String getUsername() {
		if (username == null) {
			String processId = connection.remoteProcessId();
			username = service.getUsernameForProcessId(processId);
			LOG.debug("Process " + processId + " belongs to user " + username);
		}
		return username;
	}

	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
		// Create request
		HTTPRequest httpRequest = createHttpRequest(request);
		CompletableFuture<HTTPResponse> future = new CompletableFuture<HTTPResponse>();
		responseList.put(httpRequest.getId(), future);
		this.connection.send(httpRequest);

		// Wait and get response
		HTTPResponse httpResponse = waitForResponse(httpRequest.getId());
		writeHttpResponse(httpResponse, response);
	}

	private HTTPResponse waitForResponse(Integer requestId) {
		try {
			LOG.debug("Waiting for response");
			HTTPResponse httpResponse = responseList.get(requestId).get(30, TimeUnit.SECONDS);
			responseList.remove(requestId);
			return httpResponse;
		} catch (TimeoutException e) {
			LOG.debug("Gateway Timeout");
			return HTTPResponse.newBuilder().setId(requestId).setStatus(504)
					.setBody(ByteString.copyFrom("Gateway timeout", Charset.defaultCharset())).build();
		} catch (InterruptedException | ExecutionException e) {
			LOG.error("Error while waiting for response", e);
			return HTTPResponse.newBuilder().setId(requestId).setStatus(500)
					.setBody(ByteString.copyFrom("Error", Charset.defaultCharset())).build();
		}
	}

	private void writeHttpResponse(HTTPResponse httpResponse, HttpServletResponse response) {
		// Status
		response.setStatus(httpResponse.getStatus());
		// Headers
		for (Entry<String, String> e : httpResponse.getHeadersMap().entrySet()) {
			if (!e.getKey().equals("WWW-Authenticate")) {
				response.setHeader(e.getKey(), e.getValue());
			}
		}
		// Body
		try {
			StringReader reader = new StringReader(httpResponse.getBody().toStringUtf8());
			IOUtils.copy(reader, response.getWriter());
			response.getWriter().close();
		} catch (IOException e) {
			LOG.warn("Could not write HTTP response body", e);
		}
	}

	private HTTPRequest createHttpRequest(HttpServletRequest request) {
		Builder b = HTTPRequest.newBuilder();
		// Id
		b.setId(requestIdGenerator.incrementAndGet());
		// Uri
		b.setUri(request.getRequestURI());
		// Headers
		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String key = headerNames.nextElement();
			if (!key.equals("Authorization")) {
				b.putHeaders(key, request.getHeader(key));
			}
		}
		// Method
		String method = request.getMethod();
		if (method.equals("HEAD")) {
			b.setMethod(Method.HEAD);
		} else if (method.equals("POST")) {
			b.setMethod(Method.POST);
		} else if (method.equals("PUT")) {
			b.setMethod(Method.PUT);
		} else if (method.equals("DELETE")) {
			b.setMethod(Method.DELETE);
		} else if (method.equals("TRACE")) {
			b.setMethod(Method.TRACE);
		} else if (method.equals("OPTIONS")) {
			b.setMethod(Method.OPTIONS);
		} else if (method.equals("CONNECT")) {
			b.setMethod(Method.CONNECT);
		} else if (method.equals("PATCH")) {
			b.setMethod(Method.PATCH);
		} else {
			b.setMethod(Method.GET);
		}
		// Body
		try {
			StringWriter writer = new StringWriter();
			IOUtils.copy(request.getReader(), writer);
			b.setBody(writer.toString());
		} catch (IOException e) {
			LOG.warn("Could not read HTTP request body", e);
		}
		return b.build();
	}

}