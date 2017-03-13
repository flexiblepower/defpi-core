package org.flexiblepower.service;

import java.util.Timer;

/**
 * @author Maarten Kollenstart
 * 
 * Abstract class of a publish handler that will send messages 
 * of type T to another service
 * @param <T>
 * 
 * @param <T>	Type of messages the handler will be able to serialize
 */
public abstract class PublishHandler<T> {
	protected MessageSerializer<T> serializer;
	private ServiceSession serviceSession;
	private Timer timer = new Timer();
	protected boolean active = true;
	
	public PublishHandler(ServiceSession serviceSession){
		this.serviceSession = serviceSession;
	}
	
	/**
	 * Publish a message of type T
	 * @param body	Object of type T that will be published
	 * @return		If the publish was successful or not
	 */
	public boolean publish(T body){
		if(serviceSession != null && body != null){
			return serviceSession.publish(serializer.serialize(body));
		}
		return false;
	}
	
	/**
	 * Deactive the publish handler, by deleting all 
	 * future tasks of the Timer. And setting the field
	 * active to false, so that running tasks can shut down
	 */
	public void deactivate(){
		timer.cancel();
		timer.purge();
		active = false;
	}
	
	public ServiceSession getServiceSession() {
		return serviceSession;
	}
	
	public Timer getTimer(){
		return timer;
	}
}
