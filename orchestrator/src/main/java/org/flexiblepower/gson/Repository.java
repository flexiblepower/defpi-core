package org.flexiblepower.gson;

import java.util.List;

public class Repository {
	private String name;
	private String image;
	private List<Service> tags;
	
	public Repository(){}
	
	public Repository(String name, String image, List<Service> tags) {
		super();
		this.name = name;
		this.image = image;
		this.tags = tags;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getImage() {
		return image;
	}
	public void setImage(String image) {
		this.image = image;
	}
	public List<Service> getTags() {
		return tags;
	}
	public void setTags(List<Service> tags) {
		this.tags = tags;
	}

	@Override
	public String toString() {
		return "Repository [name=" + name + ", image=" + image + ", tags=" + tags + "]";
	}
	
	

	
}
