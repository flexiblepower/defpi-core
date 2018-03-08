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
    private final Map<Integer, Widget> widgets = new HashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(0);

    @Override
    public void handle(final HttpTask httpTask) {
        final String uri = httpTask.getUri();
        for (final Entry<Integer, Widget> e : this.widgets.entrySet()) {
            final String prefix = "/" + e.getKey().toString();
            if (uri.startsWith(prefix + "/")) {
                final String relativeUri = uri.substring(prefix.length(), uri.length());
                e.getValue()
                        .handle(new HttpTask(HttpUtils.rewriteUri(httpTask.getRequest(), relativeUri),
                                (localHttpTask, response) -> httpTask.respond(response),
                                httpTask));
                return;
            }
        }
        final String path = httpTask.getPath();
        if (httpTask.getRequest().getMethod().equals(Method.GET) && path.equals("/index.html")) {
            HttpUtils.serveStaticFile(httpTask, "/dynamic/widgets/DashboardFullWidget/index.html");
        } else if (httpTask.getRequest().getMethod().equals(Method.GET) && path.equals("/menu.png")) {
            HttpUtils.serveStaticFile(httpTask, "/dynamic/widgets/DashboardFullWidget/menu.png");
        } else if (httpTask.getRequest().getMethod().equals(Method.GET) && path.equals("/script.js")) {
            HttpUtils.serveStaticFile(httpTask, "/dynamic/widgets/DashboardFullWidget/script.js");
        } else if (httpTask.getRequest().getMethod().equals(Method.POST) && path.equals("/getWidgets")) {
            httpTask.respond(this.getActiveWidgets(httpTask.getRequest()));
        } else {
            DashboardFullWidget.LOG.warn("Got request for " + httpTask.getUri()
                    + ", but dashboard only serves index.html, menu.png and script.js");
            HttpUtils.notFound(httpTask);
        }
    }

    private HTTPResponse getActiveWidgets(final HTTPRequest request) {
        final JSONObject map = new JSONObject();
        for (final Entry<Integer, Widget> e : this.widgets.entrySet()) {
            if (e.getValue().isActive()) {
                map.put(e.getKey().toString(), e.getValue().getTitle());
            }
        }
        return HTTPResponse.newBuilder()
                .setId(request.getId())
                .setStatus(200)
                .putHeaders(HttpUtils.NO_CACHE_KEY, HttpUtils.NO_CACHE_VALUE)
                .putHeaders(HttpUtils.CONTENT_TYPE, HttpUtils.APPLICATION_JAVASCRIPT)
                .setBody(ByteString.copyFromUtf8(map.toString()))
                .build();
    }

    @Override
    public String getFullWidgetId() {
        return "dashboard";
    }

    @Override
    public String getTitle() {
        return "Dashboard";
    }

    @Override
    public Type getType() {
        return Type.FULL;
    }

    public void registerSmallWidget(final Widget widget) {
        if (!widget.getType().equals(Widget.Type.SMALL)) {
            throw new IllegalArgumentException("Can only accept small widgets");
        }
        this.widgets.put(this.idGenerator.getAndIncrement(), widget);
    }

    public void unregisterSmallWidget(final Widget widget) {
        if (!widget.getType().equals(Widget.Type.SMALL)) {
            throw new IllegalArgumentException("Can only accept small widgets");
        }
        final Iterator<Entry<Integer, Widget>> it = this.widgets.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().equals(widget)) {
                it.remove();
                break;
            }
        }
    }

    @Override
    public boolean isActive() {
        return true;
    }

}
