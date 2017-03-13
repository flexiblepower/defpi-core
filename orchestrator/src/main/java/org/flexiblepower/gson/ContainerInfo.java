package org.flexiblepower.gson;

import com.google.gson.annotations.SerializedName;

public class ContainerInfo {
	private String id;
	private String state;
	private String hostId;
	@SerializedName("primaryIpAddress")
	private String ip;
	
	public ContainerInfo(){}
	
	public ContainerInfo(String id, String state, String hostId, String ip) {
		super();
		this.id = id;
		this.state = state;
		this.hostId = hostId;
		this.ip = ip;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public String getHostId() {
		return hostId;
	}
	public void setHostId(String hostId) {
		this.hostId = hostId;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	
	

}
