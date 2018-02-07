package org.flexiblepower.defpi.dashboard.controladmin.defpi;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ObsPubProcess extends DefPiProcess {

    private final List<String> subscriberProcessIds = new ArrayList<>();

    public ObsPubProcess(final DefPiConnectionAdmin defPiConnectionAdmin,
            final String processId,
            final String serviceId) {
        super(defPiConnectionAdmin, processId, serviceId);
    }

    public List<String> getSubscriberProcessIds() {
        return this.subscriberProcessIds;
    }

    public List<RmProcess> getConnectedSubscriberProcess() {
        return this.subscriberProcessIds.stream().map(id -> this.connectionAdmin.getRmProcess(id)).collect(
                Collectors.toList());
    }

    public boolean isConnectedWith(final ObsSubProcess obsSubProcess) {
        for (final String id : this.subscriberProcessIds) {
            if (id.equals(obsSubProcess.getProcessId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "ObsPubProcess [subscriberProcessIds=" + this.subscriberProcessIds + ", getSubscriberProcessIds()="
                + this.getSubscriberProcessIds() + ", getConnectedSubscriberProcess()="
                + this.getConnectedSubscriberProcess() + ", getProcessId()=" + this.getProcessId() + ", getServiceId()="
                + this.getServiceId() + "]";
    }

}
