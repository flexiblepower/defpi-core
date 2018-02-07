package org.flexiblepower.defpi.dashboard.controladmin.defpi;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CemProcess extends DefPiProcess {

    private final List<String> rmProcessIds = new ArrayList<>();

    public CemProcess(final DefPiConnectionAdmin defPiConnectionAdmin, final String processId, final String serviceId) {
        super(defPiConnectionAdmin, processId, serviceId);
    }

    public List<String> getRmProcessIds() {
        return this.rmProcessIds;
    }

    public List<RmProcess> getConnectedRmProcess() {
        return this.rmProcessIds.stream().map(id -> this.connectionAdmin.getRmProcess(id)).collect(Collectors.toList());
    }

    public boolean isConnectedWith(final RmProcess rmProcess) {
        for (final String id : this.rmProcessIds) {
            if (id.equals(rmProcess.getProcessId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "CemProcess [rmProcessId=" + this.rmProcessIds + ", getProcessId()=" + this.getProcessId()
                + ", getServiceId()=" + this.getServiceId() + "]";
    }

}
