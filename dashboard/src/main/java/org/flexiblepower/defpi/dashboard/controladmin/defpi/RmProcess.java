package org.flexiblepower.defpi.dashboard.controladmin.defpi;

public class RmProcess extends DefPiProcess {

    private String cemProcessId;

    public RmProcess(final DefPiConnectionAdmin defPiConnectionAdmin, final String processId, final String serviceId) {
        super(defPiConnectionAdmin, processId, serviceId);
    }

    public String getCemProcessId() {
        return this.cemProcessId;
    }

    public void setCemProcessId(final String cemProcessId) {
        this.cemProcessId = cemProcessId;
    }

    public CemProcess getConnectedCemProcess() {
        if (this.cemProcessId == null) {
            return null;
        }
        return this.connectionAdmin.getCemProcess(this.cemProcessId);

    }

    public boolean isConnectedWith(final CemProcess cemProcess) {
        return (this.cemProcessId != null) && this.cemProcessId.equals(cemProcess.getProcessId());
    }

    @Override
    public String toString() {
        return "RmProcess [cemProcessId=" + this.cemProcessId + ", getProcessId()=" + this.getProcessId()
                + ", getServiceId()=" + this.getServiceId() + "]";
    }

}
