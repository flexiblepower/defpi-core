package org.flexiblepower.service;

/**
 * @author Maarten Kollenstart
 *
 * Standard container class for a set of hashes and a factory
 */
public class InterfaceHandler {
	/**
	 * Hash corresponding to the description of messages the handler is able to subscribe to
	 */
	private String subscribeHash;
	/**
	 * Hash corresponding to the description of messages the handler is able to publish
	 */
	private String publishHash;
	/**
	 * The ConnectionFactory that creates the subscribe and publish handlers for 
	 * the InterfaceHandler
	 */
	private ConnectionFactory connectionFactory;
	
	public InterfaceHandler(String subscribeHash, String publishHash, ConnectionFactory connectionFactory) {
		this.subscribeHash = subscribeHash;
		this.publishHash = publishHash;
		this.connectionFactory = connectionFactory;
	}
	
	public String getSubscribeHash() {
		return subscribeHash;
	}
	public void setSubscribeHash(String subscribeHash) {
		this.subscribeHash = subscribeHash;
	}
	public ConnectionFactory getConnectionFactory() {
		return connectionFactory;
	}
	public void setConnectionFactory(ConnectionFactory connectionHandler) {
		this.connectionFactory = connectionHandler;
	}
	public String getPublishHash() {
		return publishHash;
	}
	public void setPublishHash(String publishHash) {
		this.publishHash = publishHash;
	}
}
