package org.flexiblepower.defpi.dashboard.controladmin.defpi;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ObsSubProcess extends DefPiProcess {

	private List<String> publisherProcessIds = new ArrayList<>();

	public ObsSubProcess(DefPiConnectionAdmin defPiConnectionAdmin, String processId, String serviceId) {
		super(defPiConnectionAdmin, processId, serviceId);
	}

	public List<String> getPublisherProcessIds() {
		return publisherProcessIds;
	}

	public List<RmProcess> getConnectedPublisherProcess() {
		return publisherProcessIds.stream().map(id -> connectionAdmin.getRmProcess(id)).collect(Collectors.toList());
	}

	public boolean isConnectedWith(ObsPubProcess obsPubProcess) {
		for (String id : publisherProcessIds) {
			if (id.equals(obsPubProcess.getProcessId())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return "ObsSubProcess [publisherProcessIds=" + publisherProcessIds + ", getProcessId()=" + getProcessId()
				+ ", getServiceId()=" + getServiceId() + "]";
	}

}
