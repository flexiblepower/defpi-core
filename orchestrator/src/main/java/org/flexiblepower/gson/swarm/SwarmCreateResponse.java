package org.flexiblepower.gson.swarm;

import java.util.List;

public class SwarmCreateResponse {
	private String id;
	private List<String> warnings;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public List<String> getWarnings() {
		return warnings;
	}
	public void setWarnings(List<String> warnings) {
		this.warnings = warnings;
	}
}
