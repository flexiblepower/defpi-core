package org.flexiblepower.defpi.dashboard;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPRequest;
import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPRequest.Method;
import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPResponse;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

public class DashboardFullWidget implements Widget {

	private final static Logger LOG = LoggerFactory.getLogger(DashboardFullWidget.class);
	private Map<Integer, Widget> widgets = new HashMap<>();
	private AtomicInteger idGenerator = new AtomicInteger(0);

	@Override
	public HTTPResponse handle(HTTPRequest request) {
		String uri = request.getUri();
		for (Entry<Integer, Widget> e : widgets.entrySet()) {
			String prefix = "/" + e.getKey().toString();
			if (uri.startsWith(prefix + "/")) {
				String relativeUri = uri.substring(prefix.length(), uri.length());
				return e.getValue().handle(HttpUtils.rewriteUri(request, relativeUri));
			}
		}
		String path = HttpUtils.path(uri);
		if (request.getMethod().equals(Method.GET) && path.equals("/index.html")) {
			return HttpUtils.serveStaticFile(request, "/dynamic/widgets/DashboardFullWidget/index.html");
		} else if (request.getMethod().equals(Method.GET) && path.equals("/menu.png")) {
			return HttpUtils.serveStaticFile(request, "/dynamic/widgets/DashboardFullWidget/menu.png");
		} else if (request.getMethod().equals(Method.GET) && path.equals("/script.js")) {
			return HttpUtils.serveStaticFile(request, "/dynamic/widgets/DashboardFullWidget/script.js");
		} else if (request.getMethod().equals(Method.POST) && path.equals("/getWidgets")) {
			return getWidgets(request);
		} else {
			LOG.warn("Got request for " + request.getUri()
					+ ", but dashboard only serves index.html, menu.png and script.js");
			return HttpUtils.notFound(request);
		}
	}

	private HTTPResponse getWidgets(HTTPRequest request) {
		JSONObject map = new JSONObject();
		for (Entry<Integer, Widget> e : widgets.entrySet()) {
			map.put(e.getKey().toString(), e.getValue().getTitle());
		}
		return HTTPResponse.newBuilder().setId(request.getId()).setStatus(200)
				.putHeaders(HttpUtils.NO_CACHE_KEY, HttpUtils.NO_CACHE_VALUE)
				.putHeaders(HttpUtils.CONTENT_TYPE, HttpUtils.APPLICATION_JAVASCRIPT)
				.setBody(ByteString.copyFromUtf8(map.toString())).build();
	}

	@Override
	public String getFullWidgetId() {
		return "dashboard";
	}

	@Override
	public String getTitle() {
		return "Apparaten";
	}

	@Override
	public Type getType() {
		return Type.FULL;
	}

	public void registerSmallWidget(Widget widget) {
		if (!widget.getType().equals(Widget.Type.SMALL)) {
			throw new IllegalArgumentException("Can only accept small widgets");
		}
		this.widgets.put(idGenerator.getAndIncrement(), widget);
	}

	public void unregisterSmallWidget(Widget widget) {
		if (!widget.getType().equals(Widget.Type.SMALL)) {
			throw new IllegalArgumentException("Can only accept small widgets");
		}
		Iterator<Entry<Integer, Widget>> it = widgets.entrySet().iterator();
		while (it.hasNext()) {
			if (it.next().getValue().equals(widget)) {
				it.remove();
				break;
			}
		}
	}

}
