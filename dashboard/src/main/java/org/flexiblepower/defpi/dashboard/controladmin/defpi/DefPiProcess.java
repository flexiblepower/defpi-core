package org.flexiblepower.defpi.dashboard.controladmin.defpi;

public abstract class DefPiProcess {

	protected DefPiConnectionAdmin connectionAdmin;
	private String processId;
	private String serviceId;

	public DefPiProcess(DefPiConnectionAdmin defPiConnectionAdmin, String processId, String serviceId) {
		this.connectionAdmin = defPiConnectionAdmin;
		this.processId = processId;
		this.serviceId = serviceId;
	}

	public String getProcessId() {
		return processId;
	}

	public String getServiceId() {
		return serviceId;
	}

}
