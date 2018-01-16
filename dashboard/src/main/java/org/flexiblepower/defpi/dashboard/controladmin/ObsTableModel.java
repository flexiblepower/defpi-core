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

	private DefPiConnectionAdmin connectionAdmin;

	public ObsTableModel(DefPiConnectionAdmin connectionAdmin) {
		this.connectionAdmin = connectionAdmin;
	}

	public String generateTable() {
		List<ObsPubProcess> obsPubs = new ArrayList<>();
		obsPubs.addAll(connectionAdmin.listObsPub());
		List<ObsSubProcess> obsSubs = new ArrayList<>();
		obsSubs.addAll(connectionAdmin.listObsSub());

		StringBuilder sb = new StringBuilder();
		sb.append("<table>");
		generateHearder(sb, obsSubs);

		for (ObsPubProcess obsPub : obsPubs) {
			generateRow(sb, obsPub, obsSubs);
		}

		sb.append("</table>");
		return sb.toString();
	}

	private void generateHearder(StringBuilder sb, List<ObsSubProcess> obsSubs) {
		sb.append("<tr>");
		sb.append("<td></td>");
		for (ObsSubProcess obsSub : obsSubs) {
			sb.append("<th>");
			sb.append(obsSub.getServiceId());
			sb.append("<br />(");
			sb.append(obsSub.getProcessId());
			sb.append(")");
			sb.append("</th>");
		}
		sb.append("</tr>");
	}

	private void generateRow(StringBuilder sb, ObsPubProcess obsPub, List<ObsSubProcess> obsSubs) {
		sb.append("<tr>");
		sb.append("<th>");
		sb.append(obsPub.getServiceId());
		sb.append("<br />(");
		sb.append(obsPub.getProcessId());
		sb.append(")");
		sb.append("</th>");
		for (ObsSubProcess obsSub : obsSubs) {
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

	public void handlePost(Map<String, String> data) {
		List<ObsPubProcess> obsPubs = new ArrayList<>();
		obsPubs.addAll(connectionAdmin.listObsPub());
		List<ObsSubProcess> obsSubs = new ArrayList<>();
		obsSubs.addAll(connectionAdmin.listObsSub());

		LOG.debug("Data: " + data);

		for (ObsPubProcess pub : obsPubs) {
			try {
				for (ObsSubProcess sub : obsSubs) {
					String key = pub.getProcessId() + "_" + sub.getProcessId();
					if (data.containsKey(key)) {
						LOG.debug("key " + key + " does exist, connecting");
						// should be connected
						connectionAdmin.connect(pub, sub);
					} else {
						LOG.debug("key " + key + " does not exist, disconnecting");
						// should not be connected
						connectionAdmin.disconnect(pub, sub);
					}
				}
			} catch (Exception e) {
				LOG.error("Could not modify observation connection for observation publisher " + pub.getProcessId());
			}
		}
	}

}
