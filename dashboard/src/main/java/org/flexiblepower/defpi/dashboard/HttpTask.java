package org.flexiblepower.defpi.dashboard;

import java.net.URI;

import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPRequest;
import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPResponse;

public class HttpTask {

    private final HTTPRequest request;
    private final HTTPResponseHandler responseHandler;
    private final HttpTask originalTask;
    private boolean responded;

    public HttpTask(final HTTPRequest request, final HTTPResponseHandler responseHandler, final HttpTask originalTask) {
        this.request = request;
        this.responseHandler = responseHandler;
        this.originalTask = originalTask;
        this.responded = false;
    }

    public HTTPRequest getRequest() {
        return this.request;
    }

    public String getUri() {
        return this.request.getUri();
    }

    public String getPath() {
        return URI.create(this.request.getUri()).getPath();
    }

    public String getFragment() {
        return URI.create(this.request.getUri()).getFragment();
    }

    public String getQuery() {
        return URI.create(this.request.getUri()).getQuery();
    }

    public synchronized void respond(final HTTPResponse response) {
        if (this.responded) {
            throw new IllegalStateException("Already respondend to HttpRequest with uri " + this.getUri());
        }
        this.responded = true;
        this.responseHandler.handleResponse(this, response);
    }

    public HttpTask getOriginalTask() {
        return this.originalTask;
    }

}
