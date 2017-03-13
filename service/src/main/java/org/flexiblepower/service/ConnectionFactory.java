package org.flexiblepower.service;

import com.google.protobuf.GeneratedMessage;

/**
 * @author Maarten Kollenstart
 *
 * Abstract connection factory, implementations of this class must be able to
 * create subscribehandlers and publishhandlers.
 */
public abstract class ConnectionFactory {
	/**
	 * The service that created this instance of the ConnectionFactory
	 */
	private Service service;
	
	public ConnectionFactory(Service service){
		this.service = service;
	}
	
	public abstract SubscribeHandler<?> newSubscribeHandler(ServiceSession session);
	public abstract PublishHandler<?> newPublishHandler(ServiceSession session);
	public abstract void deleteSubscribeHandler(SubscribeHandler<?> subscribeHandler);
	public abstract void deletePublishHandler(PublishHandler<?> publishHandler);

	public Service getService() {
		return service;
	}

	public void setService(Service service) {
		this.service = service;
	}
	
}
