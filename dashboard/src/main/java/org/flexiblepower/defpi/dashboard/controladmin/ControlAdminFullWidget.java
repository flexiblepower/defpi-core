package org.flexiblepower.defpi.dashboard.controladmin;

/*-
 * #%L
 * dEF-Pi dashboard
 * %%
 * Copyright (C) 2017 - 2018 Flexible Power Alliance Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.HashMap;

import org.flexiblepower.defpi.dashboard.Dashboard;
import org.flexiblepower.defpi.dashboard.HttpTask;
import org.flexiblepower.defpi.dashboard.HttpUtils;
import org.flexiblepower.defpi.dashboard.Widget;
import org.flexiblepower.defpi.dashboard.controladmin.defpi.DefPiConnectionAdmin;
import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPRequest.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlAdminFullWidget implements Widget {

    public static final Logger LOG = LoggerFactory.getLogger(ControlAdminFullWidget.class);
    @SuppressWarnings("unused")
    private final Dashboard service;
    private final DefPiConnectionAdmin connectionAdmin;

    /**
     * Auto-generated constructor for the ConnectionHandlers of the provided service
     *
     * @param service
     *            The service for which to handle the connections
     */
    public ControlAdminFullWidget(final Dashboard service) {
        this.service = service;
        this.connectionAdmin = new DefPiConnectionAdmin(service.getParameters());
    }

    private static HashMap<String, String> parseUpdatePost(final String body) {
        ControlAdminFullWidget.LOG.debug("body to parse: " + body);
        final HashMap<String, String> map = new HashMap<>();
        final String[] options = body.split("&");
        for (final String option : options) {
            if (option.contains("=")) {
                final String[] pairs = option.split("=");
                map.put(pairs[0], pairs[1]);
            }
        }
        ControlAdminFullWidget.LOG.debug("result: " + map);
        return map;
    }

    private static String stripURI(final String uri) {
        int begin = 0;
        int end = uri.length();
        if (uri.startsWith("/")) {
            begin = 1;
            end = uri.length();
        }
        if (uri.endsWith("/")) {
            end = Math.max(uri.length() - 1, 1);
        }
        return uri.substring(begin, end);
    }

    @Override
    public void handle(final HttpTask httpTask) {
        try {
            final Method method = httpTask.getRequest().getMethod();
            final String uri = ControlAdminFullWidget.stripURI(httpTask.getUri());
            if (method.equals(Method.GET)) {
                if ("index.html".equals(uri)) {
                    this.connectionAdmin.refreshData();
                    String html = HttpUtils.readTextFile("/dynamic/widgets/ControlAdminFullWidget/index.html");
                    html = html.replace("@EFITABLE@", new EfiTableModel(this.connectionAdmin).generateTable());
                    html = html.replace("@OBSTABLE@", new ObsTableModel(this.connectionAdmin).generateTable());

                    HttpUtils.serveDynamicText(httpTask, HttpUtils.TEXT_HTML, html);
                    return;
                } else if ("menu.png".equals(uri)) {
                    HttpUtils.serveStaticFile(httpTask, "/dynamic/widgets/ControlAdminFullWidget/menu.png");
                    return;
                }
            } else if (method.equals(Method.POST)) {
                if ("index.html".equals(uri)) {
                    final HashMap<String, String> postData = ControlAdminFullWidget
                            .parseUpdatePost(httpTask.getRequest().getBody());

                    ControlAdminFullWidget.LOG.debug("Received the folling POST data: " + postData);

                    this.connectionAdmin.refreshData();
                    final EfiTableModel efiTableModel = new EfiTableModel(this.connectionAdmin);
                    final ObsTableModel obsTableModel = new ObsTableModel(this.connectionAdmin);
                    efiTableModel.handlePost(postData);
                    obsTableModel.handlePost(postData);

                    String html = HttpUtils.readTextFile("/dynamic/widgets/ControlAdminFullWidget/index.html");
                    html = html.replace("@EFITABLE@", efiTableModel.generateTable());
                    html = html.replace("@OBSTABLE@", obsTableModel.generateTable());

                    HttpUtils.serveDynamicText(httpTask, HttpUtils.TEXT_HTML, html);
                    return;
                }
            }
            HttpUtils.notFound(httpTask);
        } catch (final Exception e) {
            ControlAdminFullWidget.LOG.error("Could not generate ControlAdmin response", e);
            HttpUtils.internalError(httpTask);
        }
    }

    @Override
    public String getWidgetId() {
        return "controladmin";
    }

    @Override
    public String getTitle() {
        return "Control Administration";
    }

    @Override
    public Type getType() {
        return Type.FULL_WIDGET;
    }

}
