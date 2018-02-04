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

	private DefPiConnectionAdmin connectionAdmin;

	public EfiTableModel(DefPiConnectionAdmin connectionAdmin) {
		this.connectionAdmin = connectionAdmin;
	}

	public String generateTable() {
		List<CemProcess> cems = new ArrayList<>();
		cems.addAll(connectionAdmin.listCems());
		List<RmProcess> rms = new ArrayList<>();
		rms.addAll(connectionAdmin.listRms());

		StringBuilder sb = new StringBuilder();
		sb.append("<table>");
		generateHearder(sb, cems);

		for (RmProcess rm : rms) {
			generateRow(sb, rm, cems);
		}

		sb.append("</table>");
		return sb.toString();
	}

	private void generateHearder(StringBuilder sb, List<CemProcess> cems) {
		sb.append("<tr>");
		sb.append("<td></td>");
		for (CemProcess cem : cems) {
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

	private void generateRow(StringBuilder sb, RmProcess rm, List<CemProcess> cems) {
		sb.append("<tr>");
		sb.append("<th>");
		sb.append(rm.getServiceId());
		sb.append("<br />(");
		sb.append(rm.getProcessId());
		sb.append(")");
		sb.append("</th>");
		boolean isConnected = false;
		for (CemProcess cem : cems) {
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

	public void handlePost(Map<String, String> data) {
		for (RmProcess rm : connectionAdmin.listRms()) {
			try {
				if (data.containsKey(rm.getProcessId())) {
					CemProcess cem = connectionAdmin.getCemProcess(data.get(rm.getProcessId()));
					if (cem == null) {
						// disconnect if connected
						if (rm.getCemProcessId() != null) {
							// it is connected, disconnect
							connectionAdmin.disconnect(rm.getConnectedCemProcess(), rm);
						}
					} else {
						// connect rm to cem
						connectionAdmin.connect(cem, rm);
					}
				}
			} catch (InvalidObjectIdException | AuthorizationException | NotFoundException | ConnectionException
					| IOException e) {
				LOG.error("Could not modify connection for RM " + rm.getProcessId());
			}
		}
	}

}
