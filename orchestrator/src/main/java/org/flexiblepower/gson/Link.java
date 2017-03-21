package org.flexiblepower.gson;

public class Link {
	private String id;
	private String container1;
	private String container2;
	private String interface1;
	private String interface2;
	
	public Link(){}
	
	public Link(String container1, String container2, String interface1, String interface2) {
		super();
		this.container1 = container1;
		this.container2 = container2;
		this.interface1 = interface1;
		this.interface2 = interface2;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getContainer1() {
		return container1;
	}
	public void setContainer1(String container1) {
		this.container1 = container1;
	}
	public String getContainer2() {
		return container2;
	}
	public void setContainer2(String container2) {
		this.container2 = container2;
	}
	public String getInterface1() {
		return interface1;
	}
	public void setInterface1(String interface1) {
		this.interface1 = interface1;
	}
	public String getInterface2() {
		return interface2;
	}
	public void setInterface2(String interface2) {
		this.interface2 = interface2;
	}
	
}
