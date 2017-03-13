package org.flexiblepower.gson.swarm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SwarmCreate {
	private String hostname;
	private boolean attachStdin = true;
	private boolean attachStdout = true;
	private boolean attachStderr = true;
	private boolean tty = true;
	private Map<String, Object> exposedPorts = new HashMap<>();
	private List<String> env = new ArrayList<String>();
	private String image;
	private SwarmHostConfig hostConfig;
	
	public SwarmCreate(String hostname, Map<String, String> envMap, String image, String tag, List<String> ports, SwarmHostConfig hostConfig){
		this.hostname = hostname;
		for(Entry<String, String> entry : envMap.entrySet()){
			env.add(entry.getKey()+"="+entry.getValue());
		}
		for(String port : ports){
			exposedPorts.put(port+"/tcp", new Object());
		}
		this.image = image+":"+tag;
		this.hostConfig = hostConfig;
	}
	
	public String getHostname() {
		return hostname;
	}
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
	public boolean isAttachStdin() {
		return attachStdin;
	}
	public void setAttachStdin(boolean attachStdin) {
		this.attachStdin = attachStdin;
	}
	public boolean isAttachStdout() {
		return attachStdout;
	}
	public void setAttachStdout(boolean attachStdout) {
		this.attachStdout = attachStdout;
	}
	public boolean isAttachStderr() {
		return attachStderr;
	}
	public void setAttachStderr(boolean attachStderr) {
		this.attachStderr = attachStderr;
	}
	public boolean isTty() {
		return tty;
	}
	public void setTty(boolean tty) {
		this.tty = tty;
	}
	public List<String> getEnv() {
		return env;
	}
	public void setEnv(List<String> env) {
		this.env = env;
	}
	public String getImage() {
		return image;
	}
	public void setImage(String image) {
		this.image = image;
	}
	public SwarmHostConfig getHostConfig() {
		return hostConfig;
	}
	public void setHostConfig(SwarmHostConfig hostConfig) {
		this.hostConfig = hostConfig;
	}
	public Map<String, Object> getExposedPorts() {
		return exposedPorts;
	}
	public void setExposedPorts(Map<String, Object> exposedPorts) {
		this.exposedPorts = exposedPorts;
	}
	
}
