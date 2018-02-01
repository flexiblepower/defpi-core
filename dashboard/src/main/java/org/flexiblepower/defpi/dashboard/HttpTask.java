package org.flexiblepower.defpi.dashboard;

import java.net.URI;

import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPRequest;
import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPResponse;

public class HttpTask {

	private HTTPRequest request;
	private HTTPResponseHandler responseHandler;
	private HttpTask originalTask;
	private boolean responded;

	public HttpTask(HTTPRequest request, HTTPResponseHandler responseHandler, HttpTask originalTask) {
		this.request = request;
		this.responseHandler = responseHandler;
		this.originalTask = originalTask;
		responded = false;
	}

	public HTTPRequest getRequest() {
		return this.request;
	}

	public String getUri() {
		return request.getUri();
	}

	public String getPath() {
		return URI.create(request.getUri()).getPath();
	}

	public String getFragment() {
		return URI.create(request.getUri()).getFragment();
	}

	public String getQuery() {
		return URI.create(request.getUri()).getQuery();
	}

	public synchronized void respond(HTTPResponse response) {
		if (responded) {
			throw new IllegalStateException("Already respondend to HttpRequest with uri " + getUri());
		}
		responded = true;
		responseHandler.handleResponse(this, response);
	}

	public HttpTask getOriginalTask() {
		return originalTask;
	}

}
