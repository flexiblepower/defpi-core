package org.flexiblepower.defpi.dashboard;

import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPResponse;

public interface HTTPResponseHandler {

	void handleResponse(HttpTask httpTask, HTTPResponse response);

}
