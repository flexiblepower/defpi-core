package org.flexiblepower.defpi.dashboard.controladmin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.flexiblepower.defpi.dashboard.controladmin.defpi.CemProcess;
import org.flexiblepower.defpi.dashboard.controladmin.defpi.DefPiConnectionAdmin;
import org.flexiblepower.defpi.dashboard.controladmin.defpi.RmProcess;
import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.ConnectionException;
import org.flexiblepower.exceptions.InvalidObjectIdException;
import org.flexiblepower.exceptions.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EfiTableModel {

    public static final Logger LOG = LoggerFactory.getLogger(EfiTableModel.class);

    private final DefPiConnectionAdmin connectionAdmin;

    public EfiTableModel(final DefPiConnectionAdmin connectionAdmin) {
        this.connectionAdmin = connectionAdmin;
    }

    public String generateTable() {
        final List<CemProcess> cems = new ArrayList<>();
        cems.addAll(this.connectionAdmin.listCems());
        final List<RmProcess> rms = new ArrayList<>();
        rms.addAll(this.connectionAdmin.listRms());

        final StringBuilder sb = new StringBuilder();
        sb.append("<table>");
        EfiTableModel.generateHeader(sb, cems);

        for (final RmProcess rm : rms) {
            EfiTableModel.generateRow(sb, rm, cems);
        }

        sb.append("</table>");
        return sb.toString();
    }

    private static void generateHeader(final StringBuilder sb, final List<CemProcess> cems) {
        sb.append("<tr>");
        sb.append("<td></td>");
        for (final CemProcess cem : cems) {
            sb.append("<th>");
            sb.append(cem.getServiceId());
            sb.append("<br />(");
            sb.append(cem.getProcessId());
            sb.append(")");
            sb.append("</th>");
        }
        sb.append("<th>None</th>");
        sb.append("</tr>");
    }

    private static void generateRow(final StringBuilder sb, final RmProcess rm, final List<CemProcess> cems) {
        sb.append("<tr>");
        sb.append("<th>");
        sb.append(rm.getServiceId());
        sb.append("<br />(");
        sb.append(rm.getProcessId());
        sb.append(")");
        sb.append("</th>");
        boolean isConnected = false;
        for (final CemProcess cem : cems) {
            sb.append("<td><input type=\"radio\" name=\"");
            sb.append(rm.getProcessId());
            sb.append("\" value=\"");
            sb.append(cem.getProcessId());
            sb.append("\"");
            if (rm.isConnectedWith(cem)) {
                sb.append(" checked=\"checked\"");
                isConnected = true;
            }
            sb.append(" /></td>");
        }
        sb.append("<td><input type=\"radio\" name=\"");
        sb.append(rm.getProcessId());
        sb.append("\" value=\"none\"");
        if (!isConnected) {
            sb.append(" checked=\"checked\"");
        }
        sb.append(" /></td>");
        sb.append("</tr>");
    }

    public void handlePost(final Map<String, String> data) {
        for (final RmProcess rm : this.connectionAdmin.listRms()) {
            try {
                if (data.containsKey(rm.getProcessId())) {
                    final CemProcess cem = this.connectionAdmin.getCemProcess(data.get(rm.getProcessId()));
                    if (cem == null) {
                        // disconnect if connected
                        if (rm.getCemProcessId() != null) {
                            // it is connected, disconnect
                            this.connectionAdmin.disconnect(rm.getConnectedCemProcess(), rm);
                        }
                    } else {
                        // connect rm to cem
                        this.connectionAdmin.connect(cem, rm);
                    }
                }
            } catch (InvalidObjectIdException
                    | AuthorizationException
                    | NotFoundException
                    | ConnectionException
                    | IOException e) {
                EfiTableModel.LOG.error("Could not modify connection for RM " + rm.getProcessId());
            }
        }
    }

}
