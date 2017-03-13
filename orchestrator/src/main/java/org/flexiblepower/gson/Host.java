package org.flexiblepower.gson;

import java.util.HashMap;
import java.util.Map;

public class Host {
	private String id;
	private String state;
	private String hostname;
	private String ip;
	private Map<String, String> labels;
	
	public Host(String id, String name, String ip){
		this.id = id;
		this.hostname = name;
		this.ip = ip;
		this.labels = new HashMap<String, String>();
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
	public Map<String, String> getLabels() {
		return labels;
	}
	public void setLabels(Map<String, String> labels) {
		this.labels = labels;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public String getHostname() {
		return hostname;
	}
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	@Override
	public String toString() {
		return "Host [id=" + id + ", state=" + state + ", hostname=" + hostname + ", labels=" + labels + "]";
	}
	
	
}
