package org.flexiblepower.defpi.dashboard;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.io.IOUtils;
import org.flexiblepower.defpi.dashboard.Widget.Type;
import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPRequest;
import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPResponse;

import com.google.protobuf.ByteString;

public class FullWidgetAndPageManager implements HttpHandler {

    private final List<Widget> widgets = new CopyOnWriteArrayList<>();

    @Override
    public void handle(final HttpTask httpTask) {
        final String uri = httpTask.getUri();
        for (final Widget widget : this.widgets) {
            if (widget.isActive()) {
                final String prefix = "/" + widget.getWidgetId();
                if (uri.startsWith(prefix + "/")) {
                    final String relativeUri = uri.substring(prefix.length(), uri.length());
                    String path;
                    try {
                        path = new URI(relativeUri).getPath();
                    } catch (final URISyntaxException e) {
                        HttpUtils.badRequest(httpTask, "URI not in right format");
                        return;
                    }
                    if (path.equals("/index.html") && (widget.getType() == Type.FULL_WIDGET)) {
                        // We have to add the dEF-Pi look-and-feel and menu
                        this.handleIndexFullWidget(widget, httpTask);
                        return;
                    } else {
                        widget.handle(new HttpTask(HttpUtils.rewriteUri(httpTask.getRequest(), relativeUri),
                                (httpTask1, response) -> httpTask1.getOriginalTask().respond(response),
                                httpTask));
                        return;
                    }
                }
            }
        }
        HttpUtils.notFound(httpTask);
    }

    private void handleIndexFullWidget(final Widget activeWidget, final HttpTask httpTask) {
        try (FileInputStream fis = new FileInputStream(new File("/dynamic/index.html"))) {
            final String template = IOUtils.toString(fis, Charset.defaultCharset());

            // Menu
            final StringBuilder sb = new StringBuilder();
            sb.append("<div class=\"center\"><nav><ul>");
            for (final Widget reg : this.widgets) {
                if (reg.isActive() && (reg.getType() == Widget.Type.FULL_WIDGET)) {
                    // Create menu item for full widget (not for a page)
                    if (activeWidget == reg) {
                        sb.append("<li class=\"active\">");
                    } else {
                        sb.append("<li>");
                    }
                    sb.append("<a href=\"/").append(reg.getWidgetId()).append("/index.html\">");
                    sb.append("<image src=\"/").append(reg.getWidgetId()).append("/menu.png\" />");
                    sb.append("<span>").append(reg.getTitle()).append("</span>");
                    sb.append("</a></li>");
                }
            }
            sb.append("</ul></nav></div>");

            final String body = template.replace("$menu$", sb);

            // In order to get the content of the page, we do a fake http request to the
            // active widget. When we get the response, we will inject that response in
            // the http request we were handling ourself.
            final HTTPRequest fakeHttpRequest = HTTPRequest.newBuilder()
                    .setMethod(httpTask.getRequest().getMethod())
                    .setId(0)
                    .setUri("/index.html")
                    .setBody(httpTask.getRequest().getBody())
                    .build();
            activeWidget.handle(new HttpTask(fakeHttpRequest, (fakeHttpTask, response) -> {
                final String fakeRequestResponseBody = response.getBody().toStringUtf8();
                final String newBody = body.replace("$content$", fakeRequestResponseBody);
                httpTask.respond(HTTPResponse.newBuilder()
                        .setStatus(200)
                        .setId(httpTask.getRequest().getId())
                        .putHeaders(HttpUtils.NO_CACHE_KEY, HttpUtils.NO_CACHE_VALUE)
                        .setBody(ByteString.copyFromUtf8(newBody))
                        .build());
            }, null));
        } catch (final Exception e) {
            HttpUtils.internalError(httpTask);
        }
    }

    public void registerFullWidgetOrPage(final Widget widget) {
        if (!widget.getType().equals(Widget.Type.FULL_WIDGET) && !widget.getType().equals(Widget.Type.PAGE)) {
            throw new IllegalArgumentException("Can only accept full widgets and pages");
        }
        this.widgets.add(widget);
    }

    public void unregisterFullWidgetOrPage(final Widget widget) {
        if (!widget.getType().equals(Widget.Type.FULL_WIDGET) && !widget.getType().equals(Widget.Type.PAGE)) {
            throw new IllegalArgumentException("Can only accept full widgets and pages");
        }
        this.widgets.remove(widget);
    }

}
