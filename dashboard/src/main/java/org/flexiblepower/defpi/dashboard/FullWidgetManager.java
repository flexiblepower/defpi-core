package org.flexiblepower.defpi.dashboard;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.io.IOUtils;
import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPRequest;
import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPResponse;

import com.google.protobuf.ByteString;

public class FullWidgetManager implements HttpHandler {

	private List<Widget> widgets = new CopyOnWriteArrayList<>();

	@Override
	public void handle(HttpTask httpTask) {
		String uri = httpTask.getUri();
		for (Widget widget : widgets) {
			String prefix = "/" + widget.getFullWidgetId();
			if (uri.startsWith(prefix + "/")) {
				String relativeUri = uri.substring(prefix.length(), uri.length());
				String path;
				try {
					path = new URI(relativeUri).getPath();
				} catch (URISyntaxException e) {
					HttpUtils.badRequest(httpTask, "URI not in right format");
					return;
				}
				if (path.equals("/index.html")) {
					handleIndex(widget, httpTask);
					return;
				} else {
					widget.handle(new HttpTask(HttpUtils.rewriteUri(httpTask.getRequest(), relativeUri),
							new HTTPResponseHandler() {
								@Override
								public void handleResponse(HttpTask httpTask, HTTPResponse response) {
									httpTask.getOriginalTask().respond(response);
								}
							}, httpTask));
					return;
				}
			}
		}
		HttpUtils.notFound(httpTask);
	}

	private void handleIndex(Widget activeWidget, HttpTask httpTask) {
		try {
			String template = IOUtils.toString(new FileInputStream(new File("/dynamic/index.html")),
					Charset.defaultCharset());

			// Menu
			StringBuilder sb = new StringBuilder();
			sb.append("<div class=\"center\"><nav><ul>");
			for (Widget reg : widgets) {
				if (activeWidget == reg) {
					sb.append("<li class=\"active\">");
				} else {
					sb.append("<li>");
				}
				sb.append("<a href=\"/").append(reg.getFullWidgetId()).append("/index.html\">");
				sb.append("<image src=\"/").append(reg.getFullWidgetId()).append("/menu.png\" />");
				sb.append("<span>").append(reg.getTitle()).append("</span>");
				sb.append("</a></li>");
			}
			sb.append("</ul></nav></div>");

			String body = template.replace("$menu$", sb);

			// In order to get the content of the page, we do a fake http request to the
			// active widget. When we get the response, we will inject that response in
			// the http request we were handling ourself.
			HTTPRequest fakeHttpRequest = HTTPRequest.newBuilder().setMethod(httpTask.getRequest().getMethod()).setId(0)
					.setUri("/index.html").setBody(httpTask.getRequest().getBody()).build();
			activeWidget.handle(new HttpTask(fakeHttpRequest, new HTTPResponseHandler() {

				@Override
				public void handleResponse(HttpTask fakeHttpTask, HTTPResponse response) {
					String fakeRequestResponseBody = response.getBody().toStringUtf8();
					String newBody = body.replace("$content$", fakeRequestResponseBody);
					httpTask.respond(HTTPResponse.newBuilder().setStatus(200).setId(httpTask.getRequest().getId())
							.putHeaders(HttpUtils.NO_CACHE_KEY, HttpUtils.NO_CACHE_VALUE)
							.setBody(ByteString.copyFromUtf8(newBody)).build());
				}
			}, null));
		} catch (Exception e) {
			HttpUtils.internalError(httpTask);
		}
	}

	public void registerFullWidget(Widget widget) {
		if (!widget.getType().equals(Widget.Type.FULL)) {
			throw new IllegalArgumentException("Can only accept full widgets");
		}
		this.widgets.add(widget);
	}

	public void unregisterFullWidget(Widget widget) {
		if (!widget.getType().equals(Widget.Type.FULL)) {
			throw new IllegalArgumentException("Can only accept full widgets");
		}
		this.widgets.remove(widget);
	}

}
