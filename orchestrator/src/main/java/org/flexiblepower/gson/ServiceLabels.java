package org.flexiblepower.gson;

import java.util.List;

public class ServiceLabels {
	private String name;
	private String created;
	private List<Interface> interfaces;
	private List<String> mappings;
	private List<String> ports;
	
	public ServiceLabels(){}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<Interface> getInterfaces() {
		return interfaces;
	}
	public void setInterfaces(List<Interface> interfaces) {
		this.interfaces = interfaces;
	}
	
	public String getCreated() {
		return created;
	}

	public void setCreated(String created) {
		this.created = created;
	}
	
	public List<String> getMappings() {
		return mappings;
	}

	public void setMappings(List<String> mappings) {
		this.mappings = mappings;
	}
	

	public List<String> getPorts() {
		return ports;
	}

	public void setPorts(List<String> ports) {
		this.ports = ports;
	}

	@Override
	public String toString() {
		return "Service [name=" + name + ", interfaces=" + interfaces + "]";
	}
	
}
