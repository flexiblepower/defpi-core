package org.flexiblepower.defpi.dashboard.controladmin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.flexiblepower.defpi.dashboard.controladmin.defpi.DefPiConnectionAdmin;
import org.flexiblepower.defpi.dashboard.controladmin.defpi.ObsPubProcess;
import org.flexiblepower.defpi.dashboard.controladmin.defpi.ObsSubProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObsTableModel {

    public static final Logger LOG = LoggerFactory.getLogger(ObsTableModel.class);

    private final DefPiConnectionAdmin connectionAdmin;

    public ObsTableModel(final DefPiConnectionAdmin connectionAdmin) {
        this.connectionAdmin = connectionAdmin;
    }

    public String generateTable() {
        final List<ObsPubProcess> obsPubs = new ArrayList<>();
        obsPubs.addAll(this.connectionAdmin.listObsPub());
        final List<ObsSubProcess> obsSubs = new ArrayList<>();
        obsSubs.addAll(this.connectionAdmin.listObsSub());

        final StringBuilder sb = new StringBuilder();
        sb.append("<table>");
        ObsTableModel.generateHeader(sb, obsSubs);

        for (final ObsPubProcess obsPub : obsPubs) {
            ObsTableModel.generateRow(sb, obsPub, obsSubs);
        }

        sb.append("</table>");
        return sb.toString();
    }

    private static void generateHeader(final StringBuilder sb, final List<ObsSubProcess> obsSubs) {
        sb.append("<tr>");
        sb.append("<td></td>");
        for (final ObsSubProcess obsSub : obsSubs) {
            sb.append("<th>");
            sb.append(obsSub.getServiceId());
            sb.append("<br />(");
            sb.append(obsSub.getProcessId());
            sb.append(")");
            sb.append("</th>");
        }
        sb.append("</tr>");
    }

    private static void
            generateRow(final StringBuilder sb, final ObsPubProcess obsPub, final List<ObsSubProcess> obsSubs) {
        sb.append("<tr>");
        sb.append("<th>");
        sb.append(obsPub.getServiceId());
        sb.append("<br />(");
        sb.append(obsPub.getProcessId());
        sb.append(")");
        sb.append("</th>");
        for (final ObsSubProcess obsSub : obsSubs) {
            sb.append("<td><input type=\"checkbox\" name=\"");
            sb.append(obsPub.getProcessId());
            sb.append("_");
            sb.append(obsSub.getProcessId());
            sb.append("\" value=\"true\"");
            if (obsPub.isConnectedWith(obsSub)) {
                sb.append(" checked=\"checked\"");
            }
            sb.append(" />");
            sb.append("</td>");
        }
        sb.append("</tr>");
    }

    public void handlePost(final Map<String, String> data) {
        final List<ObsPubProcess> obsPubs = new ArrayList<>();
        obsPubs.addAll(this.connectionAdmin.listObsPub());
        final List<ObsSubProcess> obsSubs = new ArrayList<>();
        obsSubs.addAll(this.connectionAdmin.listObsSub());

        ObsTableModel.LOG.debug("Data: " + data);

        for (final ObsPubProcess pub : obsPubs) {
            try {
                for (final ObsSubProcess sub : obsSubs) {
                    final String key = pub.getProcessId() + "_" + sub.getProcessId();
                    if (data.containsKey(key)) {
                        ObsTableModel.LOG.debug("key " + key + " does exist, connecting");
                        // should be connected
                        this.connectionAdmin.connect(pub, sub);
                    } else {
                        ObsTableModel.LOG.debug("key " + key + " does not exist, disconnecting");
                        // should not be connected
                        this.connectionAdmin.disconnect(pub, sub);
                    }
                }
            } catch (final Exception e) {
                ObsTableModel.LOG.error(
                        "Could not modify observation connection for observation publisher " + pub.getProcessId());
            }
        }
    }

}
