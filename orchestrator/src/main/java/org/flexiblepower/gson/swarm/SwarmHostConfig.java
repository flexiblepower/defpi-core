package org.flexiblepower.gson.swarm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SwarmHostConfig {
	private List<String> binds = new ArrayList<String>();
	private boolean priviliged;
	private List<SwarmDevices> devices = new ArrayList<SwarmDevices>();
	private String networkMode = "overlay";
	private Map<String, List<Map<String, String>>> portBindings;
	public List<String> getBinds() {
		return binds;
	}
	public void setBinds(List<String> binds) {
		this.binds = binds;
	}
	public boolean isPriviliged() {
		return priviliged;
	}
	public void setPriviliged(boolean priviliged) {
		this.priviliged = priviliged;
	}
	public List<SwarmDevices> getDevices() {
		return devices;
	}
	public void setDevices(List<SwarmDevices> devices) {
		this.devices = devices;
	}
	public String getNetworkMode() {
		return networkMode;
	}
	public void setNetworkMode(String networkMode) {
		this.networkMode = networkMode;
	}
	public Map<String, List<Map<String, String>>> getPortBindings() {
		return portBindings;
	}
	public void setPortBindings(Map<String, List<Map<String, String>>> portBindings) {
		this.portBindings = portBindings;
	}
	
	
}
