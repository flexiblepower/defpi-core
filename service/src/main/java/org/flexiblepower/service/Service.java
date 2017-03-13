package org.flexiblepower.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.flexiblepower.service.proto.SessionProto.Session;
import org.reflections.Reflections;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

import com.google.protobuf.InvalidProtocolBufferException;

public abstract class Service {
	/**
	 * Active sessions of the current service
	 */
	public static Map<String, ServiceSession> sessions = new HashMap<String, ServiceSession>();

	/**
	 * Available handlers for the different interfaces
	 */
	protected Map<String, InterfaceHandler> handlers = new HashMap<String, InterfaceHandler>();

	/**
	 * Main loop to receive Session messages from the orchestrator
	 */
	public void start() {
		Context ctx = ZMQ.context(1);
		Socket socket = ctx.socket(ZMQ.REP);
		socket.bind("tcp://*:4999");
		while (true) {
			final byte[] data = socket.recv(0);
			System.out.println("Received data");
			try {
				Session session = Session.parseFrom(data);
				System.out.println(session);
				boolean result;
				if(session.getMode() == Session.ModeType.CREATE){
					result = createSession(session);
				}else{
					result = removeSession(session);
				}
				socket.send(new byte[] { (byte) (result ? 0 : 1) });
			} catch (InvalidProtocolBufferException e) {
				socket.send(new byte[] { (byte) 1 });
				e.printStackTrace();
			}
		}
	}

	/**
	 * Create new session based on the given Session message
	 * @param session	Session protobuf deserialized message
	 * @return			Whether or not the session is created
	 */
	private boolean createSession(Session session) {
		System.out.println("Received session create message: \n" 
				+ "    id           : " + session.getId() + "\n"
				+ "    port         : " + session.getPort() + "\n" 
				+ "    subscribeHash: " + session.getSubscribeHash()+ "\n" 
				+ "    publishHash  : " + session.getPublishHash());
		ConnectionFactory connectionFactory = getFactoryFromHash(session.getSubscribeHash(), session.getPublishHash());
		if (connectionFactory != null) {
			ServiceSession serviceSession = new ServiceSession(session, connectionFactory);
			sessions.put(session.getId(), serviceSession);
			serviceSession.startConsuming();
			return true;
		}
		return false;
	}

	/**
	 * Remove session identified by the Session message
	 * @param session	Session protobuf deserialized message
	 * @return			Whether or not the session is removed
	 */
	private boolean removeSession(Session session) {
		System.out.println("Received session remove message: \n" 
				+ "    id: " + session.getId());
		ServiceSession serviceSession = sessions.get(session.getId());
		if (serviceSession != null) {
			System.out.println("Stopping session");
			serviceSession.stopSession();
			return true;
		}
		sessions.remove(session.getId());
		return false;
	}

	/**
	 * Get the ConnectionFactory corresponding to a specific interface
	 * @param interfaceName	The human readable name of the interface
	 * @return				The corresponding ConnectionFactory, will return null when no factory is found
	 */
	public ConnectionFactory getFactory(String interfaceName) {
		InterfaceHandler handler = handlers.get(interfaceName);
		if(handler == null) return null;
		
		return handler.getConnectionFactory();
	}
	
	
	/**
	 * Get the ConnectionFactory corresponding to a set of hashes
	 * @param subscribeHash	The subscribe hash the factory has to support
	 * @param publishHash	The publish hash the factory has to support
	 * @return				The corresponding ConnectionFactory, will return null when no factory is found
	 */
	public ConnectionFactory getFactoryFromHash(String subscribeHash, String publishHash) {
		for (Map.Entry<String, InterfaceHandler> handler : handlers.entrySet()) {
			if(handler.getValue().getSubscribeHash().equals(subscribeHash) && 
					handler.getValue().getPublishHash().equals(publishHash)){
				return handler.getValue().getConnectionFactory();
			}
		}
		return null;
	}

	/**
	 * Find factory based on reflection and the Factory annotation. The prefix (i.e. the package) 
	 * is the location the code will look for factories and the interface name is the matcher for 
	 * the annotation.
	 * 
	 * @param prefix			The prefix (i.e. the package) is the location the code will look for factories
	 * @param interfaceName		The interface will be used for matching the annotation
	 * @return					A class that extends ConnectionFactory or else null
	 */
	protected Class<? extends ConnectionFactory> getFactory(String prefix, String interfaceName) {
		Reflections reflections = new Reflections(prefix);
		Set<Class<?>> factories = reflections.getTypesAnnotatedWith(Factory.class);
		for (Class<?> factory : factories) {
			Factory f = factory.getAnnotation(Factory.class);
			if (f.interfaceName().equalsIgnoreCase(interfaceName)) {
				return (Class<? extends ConnectionFactory>) factory;
			}
		}
		return null;
	}
}
