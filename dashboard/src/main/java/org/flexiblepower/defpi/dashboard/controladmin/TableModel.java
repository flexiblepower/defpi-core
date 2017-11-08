package org.flexiblepower.defpi.dashboard.controladmin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

import org.flexiblepower.model.Connection;
import org.flexiblepower.model.Process;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TableModel {

	public static final Logger LOG = LoggerFactory.getLogger(TableModel.class);

	HashMap<Process, List<Connection>> colProcessMap = new HashMap<>();
	HashMap<Process, List<Connection>> rowProcessMap = new HashMap<>();
	private String path;

	public TableModel(HashMap<Process, List<Connection>> colProcessMap,
			HashMap<Process, List<Connection>> rowProcessMap, String path) {
		this.colProcessMap = colProcessMap;
		this.rowProcessMap = rowProcessMap;
		this.path = path;
	}

	public String generateHeaders() {
		String header = "<thead><tr><th></th>";
		String footer = "</tr></thead>";
		String processColumns = "";
		for (Process process : colProcessMap.keySet()) {
			processColumns += "<th>" + process.getServiceId() + "(" + process.getId() + ")</th>";
		}
		String noneColumn = "<th>None</th>";

		return header + processColumns + noneColumn + footer;
	}

	public String generateRows(String type) {
		String rows = "";
		int i = 1;
		for (Process rowProcess : rowProcessMap.keySet()) {
			rows += "<tr><td>" + rowProcess.getServiceId() + "(" + rowProcess.getId() + ")</td>";
			boolean conn = false;
			for (Process colProcess : colProcessMap.keySet()) {
				rows += "<td><input type=\"" + type + "\" name=\"optradio" + i + "\" "
						+ (isConnected(colProcess, rowProcess) ? "checked=\"checked\" " : "") + "value=\""
						+ colProcess.getId() + "_" + rowProcess.getId() + "\"></td>";
				conn |= isConnected(colProcess, rowProcess);
			}
			rows += "<td><input type=\"radio\" name=\"optradio" + i + "\" " + (!conn ? "checked=\"checked\" " : "")
					+ "value=\"None_" + rowProcess.getId() + "\"></td></tr>";
			i++;
		}
		return "<tbody>" + rows
				+ "<input class=\"btn btn-primary\" form=\"theForm\" type=\"submit\" value=\"Update\"></tbody>";
	}

	private boolean isConnected(Process colProcess, Process rowProcess) {
		LOG.debug("Is " + colProcess.getId() + " connected to " + rowProcess.getId() + " ?");
		for (Connection conn : colProcessMap.get(colProcess)) {
			if ((conn.getEndpoint1().getProcessId().toString().equals(colProcess.getId().toString())
					&& conn.getEndpoint2().getProcessId().toString().equals(rowProcess.getId().toString()))
					|| (conn.getEndpoint2().getProcessId().toString().equals(colProcess.getId().toString())
							&& conn.getEndpoint1().getProcessId().toString().equals(rowProcess.getId().toString()))) {
				LOG.debug("Yes!");
				return true;
			}
		}
		LOG.debug("No!");
		return false;
	}

	public String getHTMLTable(String type) {
		String html = null;
		try {
			byte[] encoded = Files.readAllBytes(Paths.get(path));
			html = new String(encoded, "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (html != null) {
			html = html.replace("@HEADER@", generateHeaders());
			html = html.replace("@ROWS@", generateRows(type));
			LOG.debug(html);
			return html;
		} else {
			return null;
		}
	}
}
