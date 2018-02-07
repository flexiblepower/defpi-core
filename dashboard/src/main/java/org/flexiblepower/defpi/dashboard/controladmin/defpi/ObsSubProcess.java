package org.flexiblepower.defpi.dashboard.controladmin.defpi;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ObsSubProcess extends DefPiProcess {

    private final List<String> publisherProcessIds = new ArrayList<>();

    public ObsSubProcess(final DefPiConnectionAdmin defPiConnectionAdmin,
            final String processId,
            final String serviceId) {
        super(defPiConnectionAdmin, processId, serviceId);
    }

    public List<String> getPublisherProcessIds() {
        return this.publisherProcessIds;
    }

    public List<RmProcess> getConnectedPublisherProcess() {
        return this.publisherProcessIds.stream().map(id -> this.connectionAdmin.getRmProcess(id)).collect(
                Collectors.toList());
    }

    public boolean isConnectedWith(final ObsPubProcess obsPubProcess) {
        for (final String id : this.publisherProcessIds) {
            if (id.equals(obsPubProcess.getProcessId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "ObsSubProcess [publisherProcessIds=" + this.publisherProcessIds + ", getProcessId()="
                + this.getProcessId() + ", getServiceId()=" + this.getServiceId() + "]";
    }

}
