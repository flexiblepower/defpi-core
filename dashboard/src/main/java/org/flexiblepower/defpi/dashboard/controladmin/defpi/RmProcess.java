package org.flexiblepower.defpi.dashboard.controladmin.defpi;

public class RmProcess extends DefPiProcess {

	private String cemProcessId;

	public RmProcess(DefPiConnectionAdmin defPiConnectionAdmin, String processId, String serviceId) {
		super(defPiConnectionAdmin, processId, serviceId);
	}

	public String getCemProcessId() {
		return cemProcessId;
	}

	public void setCemProcessId(String cemProcessId) {
		this.cemProcessId = cemProcessId;
	}

	public CemProcess getConnectedCemProcess() {
		if (cemProcessId == null) {
			return null;
		}
		return connectionAdmin.getCemProcess(this.cemProcessId);

	}

	public boolean isConnectedWith(CemProcess cemProcess) {
		return cemProcessId != null && cemProcessId.equals(cemProcess.getProcessId());
	}

	@Override
	public String toString() {
		return "RmProcess [cemProcessId=" + cemProcessId + ", getProcessId()=" + getProcessId() + ", getServiceId()="
				+ getServiceId() + "]";
	}

}
