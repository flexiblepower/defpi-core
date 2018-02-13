package org.flexiblepower.defpi.dashboard.controladmin;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import org.flexiblepower.defpi.dashboard.Dashboard;
import org.flexiblepower.defpi.dashboard.HttpTask;
import org.flexiblepower.defpi.dashboard.HttpUtils;
import org.flexiblepower.defpi.dashboard.Widget;
import org.flexiblepower.defpi.dashboard.controladmin.defpi.CemProcess;
import org.flexiblepower.defpi.dashboard.controladmin.defpi.DefPiConnectionAdmin;
import org.flexiblepower.defpi.dashboard.controladmin.defpi.RmProcess;
import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPRequest.Method;
import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.ConnectionException;
import org.flexiblepower.exceptions.InvalidObjectIdException;
import org.flexiblepower.exceptions.NotFoundException;
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

    private static boolean efiIsConnected(final DefPiConnectionAdmin connectionAdmin) {
        for (final CemProcess cem : connectionAdmin.listCems()) {
            if (cem.getRmProcessIds().isEmpty()) {
                return false;
            } else {
                return true;
            }
        }
        // Als er geen CEMs zijn gaan we er maar vanuit dat er wel EFI connecties zouden
        // moeten zijn
        return true;
    }

    private void connectAllEfiSessions() {
        // For now we assume there is 0 or 1 CEM
        if (this.connectionAdmin.listCems().isEmpty()) {
            return;
        }
        try {
            final CemProcess cem = this.connectionAdmin.listCems().iterator().next();
            for (final RmProcess rm : this.connectionAdmin.listRms()) {
                this.connectionAdmin.connect(cem, rm);
            }
        } catch (ConnectionException | AuthorizationException | NotFoundException | IOException e) {
            ControlAdminFullWidget.LOG.error("Could not create EFI connection", e);
        }
    }

    private void disconnectAllEfiSessions() {
        // For now we assumer there is 0 or 1 CEM
        if (this.connectionAdmin.listCems().isEmpty()) {
            return;
        }
        try {
            final CemProcess cem = this.connectionAdmin.listCems().iterator().next();
            for (final RmProcess rm : this.connectionAdmin.listRms()) {
                this.connectionAdmin.disconnect(cem, rm);
            }
        } catch (UnsupportedEncodingException
                | InvalidObjectIdException
                | AuthorizationException
                | NotFoundException e) {
            ControlAdminFullWidget.LOG.error("Could not remove EFI connection", e);
        }
    }

    @Override
    public void handle(final HttpTask httpTask) {
        try {
            final Method method = httpTask.getRequest().getMethod();
            final String uri = ControlAdminFullWidget.stripURI(httpTask.getUri());
            if (method.equals(Method.GET)) {
                if ("index.html".equals(uri)) {
                    this.servePackageOptions(httpTask);
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

                    if (postData.containsKey("option")) {
                        final String value = postData.get("option");
                        if ("sympower".equals(value)) {
                            ControlAdminFullWidget.LOG.info("Creating EFI sessions");
                            this.connectAllEfiSessions();
                        } else if ("alleenmeten".equals(value)) {
                            ControlAdminFullWidget.LOG.info("Removing EFI sessions");
                            this.service.publishUserDecisionObservation("alleen meten");
                            this.disconnectAllEfiSessions();
                        }
                    }

                    this.servePackageOptions(httpTask);
                    return;
                }
            }
            HttpUtils.notFound(httpTask);
        } catch (final Exception e) {
            ControlAdminFullWidget.LOG.error("Could not generate ControlAdmin response", e);
            HttpUtils.internalError(httpTask);
        }
    }

    private void servePackageOptions(final HttpTask httpTask) throws UnsupportedEncodingException,
            AuthorizationException,
            IOException {
        this.connectionAdmin.refreshData();
        String html = HttpUtils.readTextFile("/dynamic/widgets/ControlAdminFullWidget/index.html");

        if (ControlAdminFullWidget.efiIsConnected(this.connectionAdmin)) {
            html = html.replace("@SYMPOWER_STYLE@", "optionactive");
            html = html.replace("@ALLEENMETEN_STYLE@", "optionnotactive");
        } else {
            html = html.replace("@SYMPOWER_STYLE@", "optionnotactive");
            html = html.replace("@ALLEENMETEN_STYLE@", "optionactive");
        }

        HttpUtils.serveDynamicText(httpTask, HttpUtils.TEXT_HTML, html);
    }

    @Override
    public String getFullWidgetId() {
        return "controladmin";
    }

    @Override
    public String getTitle() {
        return "Sturende partij";
    }

    @Override
    public Type getType() {
        return Type.FULL;
    }

    @Override
    public boolean isActive() {
        return true;
    }

}