package org.flexiblepower.defpi.dashboard;

import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPRequest;
import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpRouter implements HttpHandler {

	private final static Logger LOG = LoggerFactory.getLogger(HttpRouter.class);

	private HttpStaticContentHandler staticContentHandler;
	private FullWidgetManager fullWidgetManager;

	public HttpRouter(FullWidgetManager fullWidgetManager) {
		this.fullWidgetManager = fullWidgetManager;
		this.staticContentHandler = new HttpStaticContentHandler();
	}

	@Override
	public HTTPResponse handle(HTTPRequest request) {
		LOG.info(request.getMethod() + ": " + request.getUri());

		if (request.getUri().equals("/")) {
			return HTTPResponse.newBuilder().setId(request.getId()).putHeaders("Location", "/dashboard/index.html")
					.setStatus(301).build();
		}

		HTTPResponse response = fullWidgetManager.handle(request);
		if (response.getStatus() == 404) {
			return staticContentHandler.handle(request);
		} else {
			return response;
		}
	}

}
