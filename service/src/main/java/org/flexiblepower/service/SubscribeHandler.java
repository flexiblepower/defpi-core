package org.flexiblepower.service;

import java.util.Timer;

/**
 * Abstract class of a subscribe handler that will receive messages of type T
 * 
 * @author Maarten Kollenstart
 *
 * @param <T>
 *            Type of messages the class will be able to deserialize
 */
public abstract class SubscribeHandler<T> {
	/**
	 * Session the instance of the subscribe handler is connected to.
	 */
	private ServiceSession serviceSession;

	/**
	 * Timer that can be used by the handler to execute tasks.
	 */
	private Timer timer = new Timer();

	/**
	 * Whether or not de subscribe handler is still active, can be used as guard
	 * in a infinite loop so that connections can be closed safely.
	 */
	protected boolean active = true;

	protected MessageSerializer<T> serializer;

	/**
	 * Constructor that needs a Class for the protobuf parser and a
	 * ServiceSession object.
	 * 
	 * @param cls
	 *            Class of Protocol Buffer message for deserialization
	 * @param serviceSession
	 *            ServiceSession corresponding to the current instance of the
	 *            handler
	 */
	public SubscribeHandler(ServiceSession serviceSession) {
		this.serviceSession = serviceSession;
	}

	/**
	 * Method that will be called when a message is received. To be implemented
	 * by developers that write their own handlers
	 * 
	 * @param message
	 *            The deserialized message of type T
	 */
	public abstract void receiveMessage(T message);

	/**
	 * Receive serialized byte array
	 * 
	 * @param data
	 *            The serialized message byte array
	 */
	public void receiveMessage(byte[] data) {
		T object = serializer.deserialize(data);
		if(object != null)
			receiveMessage(object);
	}

	/**
	 * Deactive the subscribe handler, by deleting all future tasks of the
	 * Timer. And setting the field active to false, so that running tasks can
	 * shut down
	 */
	public void deactivate() {
		timer.cancel();
		timer.purge();
		active = false;
	}

	public ServiceSession getServiceSession() {
		return serviceSession;
	}

	public Timer getTimer() {
		return timer;
	}
}
