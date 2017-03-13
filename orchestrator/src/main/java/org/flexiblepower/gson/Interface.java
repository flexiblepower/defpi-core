package org.flexiblepower.gson;

public class Interface {
	private String name;
	private int cardinality;
	private boolean autoConnect;
	private String subscribeHash;
	private String publishHash;
	
	public Interface(){}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getCardinality() {
		return cardinality;
	}
	public void setCardinality(int cardinality) {
		this.cardinality = cardinality;
	}
	public boolean isAutoConnect() {
		return autoConnect;
	}
	public void setAutoConnect(boolean autoConnect) {
		this.autoConnect = autoConnect;
	}
	public String getSubscribeHash() {
		return subscribeHash;
	}
	public void setSubscribeHash(String subscribeHash) {
		this.subscribeHash = subscribeHash;
	}
	public String getPublishHash() {
		return publishHash;
	}
	public void setPublishHash(String publishHash) {
		this.publishHash = publishHash;
	}
	@Override
	public String toString() {
		return "Interface [name=" + name + ", cardinality=" + cardinality + ", autoConnect=" + autoConnect
				+ ", subscribeHash=" + subscribeHash + ", publishHash=" + publishHash + "]";
	}
	
}