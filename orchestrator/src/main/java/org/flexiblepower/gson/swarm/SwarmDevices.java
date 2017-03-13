package org.flexiblepower.gson.swarm;

public class SwarmDevices {
	private String pathOnHost;
	private String pathInContainer;
	private String cgroupPermissions;
	
	public SwarmDevices(String device){
		this.pathOnHost = this.pathInContainer = device;
		this.cgroupPermissions = "mrw";
	}
	
	public String getPathInContainer() {
		return pathInContainer;
	}
	public void setPathInContainer(String pathInContainer) {
		this.pathInContainer = pathInContainer;
	}
	public String getPathOnHost() {
		return pathOnHost;
	}
	public void setPathOnHost(String pathOnHost) {
		this.pathOnHost = pathOnHost;
	}
	public String getCgroupPermissions() {
		return cgroupPermissions;
	}
	public void setCgroupPermissions(String cgroupPermissions) {
		this.cgroupPermissions = cgroupPermissions;
	}
	
}
