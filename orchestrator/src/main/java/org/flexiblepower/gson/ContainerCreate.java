package org.flexiblepower.gson;

import java.util.HashMap;
import java.util.Map;

public class ContainerCreate {
	private String name;
	private String description;
	private String imageUuid;
	private String tag;
	private Map<String, String> labels;
	private Map<String, String> environment;
	private String requestedHostId;
	
	public ContainerCreate(){}
	
	public ContainerCreate(String name, String description, String imageUuid, String tag, Map<String, String> environment) {
		super();
		this.name = name;
		this.description = description;
		this.imageUuid = imageUuid;
		this.tag = tag;
		this.labels = new HashMap<String, String>();
		labels.put("io.rancher.container.pull_image", "always");
		this.environment = environment;
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
	public String getImageUuid() {
		return imageUuid;
	}
	public void setImageUuid(String imageUuid) {
		this.imageUuid = imageUuid;
	}
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}
	public Map<String, String> getLabels() {
		return labels;
	}
	public void setLabels(Map<String, String> labels) {
		this.labels = labels;
	}
	public Map<String, String> getEnvironment() {
		return environment;
	}
	public void setEnvironment(Map<String, String> environment) {
		this.environment = environment;
	}
	public String getRequestedHostId() {
		return requestedHostId;
	}
	public void setRequestedHostId(String requestedHostId) {
		this.requestedHostId = requestedHostId;
	}
	
	
}

/*
JSONObject message = new JSONObject().accumulate("description", cd.getDescription())
				.accumulate("imageUuid", cd.getUuid())
				.accumulate("name", cd.getName())
				.accumulate("labels", (new JSONObject()).accumulate("io.rancher.container.pull_image", "always"))
				.accumulate("environment", env);
		if(!cd.getHost().equals("")){
			message.accumulate("requestedHostId", cd.getHost());
		}
*/