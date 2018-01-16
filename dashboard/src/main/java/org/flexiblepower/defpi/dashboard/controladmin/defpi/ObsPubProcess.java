package org.flexiblepower.defpi.dashboard.controladmin.defpi;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ObsPubProcess extends DefPiProcess {

	private List<String> subscriberProcessIds = new ArrayList<>();

	public ObsPubProcess(DefPiConnectionAdmin defPiConnectionAdmin, String processId, String serviceId) {
		super(defPiConnectionAdmin, processId, serviceId);
	}

	public List<String> getSubscriberProcessIds() {
		return subscriberProcessIds;
	}

	public List<RmProcess> getConnectedSubscriberProcess() {
		return subscriberProcessIds.stream().map(id -> connectionAdmin.getRmProcess(id)).collect(Collectors.toList());
	}

	public boolean isConnectedWith(ObsSubProcess obsSubProcess) {
		for (String id : subscriberProcessIds) {
			if (id.equals(obsSubProcess.getProcessId())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return "ObsPubProcess [subscriberProcessIds=" + subscriberProcessIds + ", getSubscriberProcessIds()="
				+ getSubscriberProcessIds() + ", getConnectedSubscriberProcess()=" + getConnectedSubscriberProcess()
				+ ", getProcessId()=" + getProcessId() + ", getServiceId()=" + getServiceId() + "]";
	}

}
