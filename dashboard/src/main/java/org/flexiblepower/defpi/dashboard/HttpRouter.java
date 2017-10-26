package org.flexiblepower.defpi.dashboard;

import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPRequest;
import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpRouter implements HttpHandler {

	private final static Logger LOG = LoggerFactory.getLogger(HttpRouter.class);

	private HttpStaticContentHandler staticContentHandler;

	public HttpRouter() {
		this.staticContentHandler = new HttpStaticContentHandler();
	}

	@Override
	public HTTPResponse handle(HTTPRequest request) {
		LOG.info(request.getMethod() + ": " + request.getUri());

		return staticContentHandler.handle(request);
	}

}
