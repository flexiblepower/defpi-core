package org.flexiblepower.defpi.dashboard.controladmin.defpi;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CemProcess extends DefPiProcess {

	private List<String> rmProcessIds = new ArrayList<>();

	public CemProcess(DefPiConnectionAdmin defPiConnectionAdmin, String processId, String serviceId) {
		super(defPiConnectionAdmin, processId, serviceId);
	}

	public List<String> getRmProcessIds() {
		return rmProcessIds;
	}

	public List<RmProcess> getConnectedRmProcess() {
		return rmProcessIds.stream().map(id -> connectionAdmin.getRmProcess(id)).collect(Collectors.toList());
	}

	public boolean isConnectedWith(RmProcess rmProcess) {
		for (String id : rmProcessIds) {
			if (id.equals(rmProcess.getProcessId())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return "CemProcess [rmProcessId=" + rmProcessIds + ", getProcessId()=" + getProcessId() + ", getServiceId()="
				+ getServiceId() + "]";
	}

}
