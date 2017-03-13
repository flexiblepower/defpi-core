package org.flexiblepower.gson;

import java.util.List;

public class Service {
	private String name;
	private List<Interface> interfaces;
	private List<String> mappings;
	private List<String> ports;
	private String image;
	private String tag;
	private String created;
	private String platform;
	
	public Service(){}
	
	public Service(String name, List<Interface> interfaces, List<String> mappings, List<String> ports, String image, String tag, String created) {
		super();
		this.name = name;
		this.interfaces = interfaces;
		this.mappings = mappings;
		this.ports = ports;
		this.image = image;
		this.tag = tag;
		this.created = created;
		if(this.tag.endsWith("-arm")){
			this.platform = "arm";
		}else{
			this.platform = "x86";
		}
	}
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
	public List<String> getMappings() {
		return mappings;
	}
	public List<String> getPorts() {
		return ports;
	}

	public void setPorts(List<String> ports) {
		this.ports = ports;
	}

	public void setMappings(List<String> mappings) {
		this.mappings = mappings;
	}
	public String getImage() {
		return image;
	}
	public void setImage(String image) {
		this.image = image;
	}
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getCreated() {
		return created;
	}

	public void setCreated(String created) {
		this.created = created;
	}

	@Override
	public String toString() {
		return "Service [name=" + name + ", interfaces=" + interfaces + ", image=" + image + ", tag=" + tag + ", created="
				+ created + "]";
	}
	
	
}