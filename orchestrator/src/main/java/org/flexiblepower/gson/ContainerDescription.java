package org.flexiblepower.gson;

import java.util.Map;

public class ContainerDescription {
	private String name;
	private String description;
	private String image;
	private String tag;
	private String host;
	private Map<String, String> environment;
	private String uuid;
	
	public ContainerDescription(){}
	
	public ContainerDescription(String name, String description, String image, String tag, String host,
			Map<String, String> environment, String uuid) {
		super();
		this.name = name;
		this.description = description;
		this.image = image;
		this.host = host;
		this.environment = environment;
		this.uuid = uuid;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
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
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public Map<String, String> getEnvironment() {
		return environment;
	}
	public void setEnvironment(Map<String, String> environment) {
		this.environment = environment;
	}
	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	@Override
	public String toString() {
		return "ContainerDescription [name=" + name + ", description=" + description + ", image=" + image + ", tag="
				+ tag + ", host=" + host + ", environment=" + environment + ", uuid=" + uuid + "]";
	}
	
}