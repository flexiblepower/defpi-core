package org.flexiblepower.defpi.dashboard;

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
	public void handle(HttpTask httpTask) {
		LOG.info(httpTask.getRequest().getMethod() + ": " + httpTask.getRequest().getUri());

		// Rewrite?
		if (httpTask.getPath().equals("/")) {
			httpTask.respond(HTTPResponse.newBuilder().setId(httpTask.getRequest().getId())
					.putHeaders("Location", "/dashboard/index.html").setStatus(301).build());
			return;
		}

		// Dynamic?
		fullWidgetManager.handle(new HttpTask(httpTask.getRequest(), new HTTPResponseHandler() {
			@Override
			public void handleResponse(HttpTask httpTask, HTTPResponse response) {
				// If the dynamic handler could not handle the request, try to serve static
				// content
				if (response.getStatus() == 404) {
					staticContentHandler.handle(httpTask.getOriginalTask());
				} else {
					// Just answer
					httpTask.getOriginalTask().respond(response);
				}
			}

		}, httpTask));
	}

}
