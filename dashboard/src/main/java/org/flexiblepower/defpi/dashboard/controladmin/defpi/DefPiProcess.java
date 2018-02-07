package org.flexiblepower.defpi.dashboard.controladmin.defpi;

public abstract class DefPiProcess {

    protected DefPiConnectionAdmin connectionAdmin;
    private final String processId;
    private final String serviceId;

    public DefPiProcess(final DefPiConnectionAdmin defPiConnectionAdmin,
            final String processId,
            final String serviceId) {
        this.connectionAdmin = defPiConnectionAdmin;
        this.processId = processId;
        this.serviceId = serviceId;
    }

    public String getProcessId() {
        return this.processId;
    }

    public String getServiceId() {
        return this.serviceId;
    }

}
