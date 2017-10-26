package org.flexiblepower.defpi.dashboard;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.io.IOUtils;
import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPRequest;
import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPRequest.Method;
import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPResponse;

import com.google.protobuf.ByteString;

public class FullWidgetManager implements HttpHandler {

	private List<Widget> widgets = new CopyOnWriteArrayList<>();

	@Override
	public HTTPResponse handle(HTTPRequest request) {
		String uri = request.getUri();
		for (Widget widget : widgets) {
			String prefix = "/" + widget.getName();
			if (uri.startsWith(prefix + "/")) {
				String relativeUri = uri.substring(prefix.length(), uri.length());
				if (relativeUri.startsWith("/index.html")) {
					return handleIndex(widget, request);
				} else {
					return widget.handle(HttpUtils.rewriteUri(request, relativeUri));
				}
			}
		}
		return HttpUtils.notFound(request);
	}

	private HTTPResponse handleIndex(Widget activeWidget, HTTPRequest request) {
		try {
			String template = IOUtils.toString(new FileInputStream(new File("/dynamic/index.html")),
					Charset.defaultCharset());

			StringBuilder sb = new StringBuilder();
			sb.append("<div class=\"center\"><nav><ul>");
			for (Widget reg : widgets) {
				if (activeWidget == reg) {
					sb.append("<li class=\"active\">");
				} else {
					sb.append("<li>");
				}
				sb.append("<a href=\"/").append(reg.getName()).append("/index.html\">");
				sb.append("<image src=\"/").append(reg.getName()).append("/menu.png\" />");
				sb.append("<span>").append(reg.getTitle()).append("</span>");
				sb.append("</a></li>");
			}
			sb.append("</ul></nav></div>");

			String body = template.replace("$menu$", sb);

			body = body.replace("$content$", activeWidget
					.handle(HTTPRequest.newBuilder().setMethod(Method.GET).setId(0).setUri("/index.html").build())
					.getBody().toStringUtf8());

			return HTTPResponse.newBuilder().setStatus(200).setId(request.getId())
					.putHeaders(HttpUtils.NO_CACHE_KEY, HttpUtils.NO_CACHE_VALUE).setBody(ByteString.copyFromUtf8(body))
					.build();
		} catch (Exception e) {
			return HttpUtils.internalError(request);
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
