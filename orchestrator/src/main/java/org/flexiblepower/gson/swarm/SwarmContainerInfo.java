package org.flexiblepower.gson.swarm;

public class SwarmContainerInfo {
	private String id;
	private String ip;
	private String node;
	private String state;

	public SwarmContainerInfo(String id, String ip, String node, String state) {
		this.id = id;
		this.ip = ip;
		this.node = node;
		this.state = state;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getNode() {
		return node;
	}

	public void setNode(String node) {
		this.node = node;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	@Override
	public String toString() {
		return "SwarmContainerInfo [id=" + id + ", ip=" + ip + ", node=" + node + ", state=" + state + "]";
	}
	
}