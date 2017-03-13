package org.flexiblepower.service;

import org.flexiblepower.service.proto.SessionProto.Session;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQException;

import zmq.ZError;

public class ServiceSession {
	/**
	 * ConnectionFactory that is used to retrieve manage subscribe and publish handlers
	 */
	private ConnectionFactory factory;
	/**
	 * SubscribeHandler that is used for receiving messages in this session
	 */
	private SubscribeHandler<?> subscribeHandler;
	/**
	 * PublishHandler that is used for sending messages in this session
	 */
	private PublishHandler<?> publishHandler;
	/**
	 * ZeroMQ context
	 */
	private Context ctx;
	/**
	 * ZeroMQ socket to receive messages
	 */
	private Socket subscribeSocket;
	/**
	 * ZeroMQ socket to publish messages
	 */
	private Socket publishSocket;
	/**
	 * Timeout for sending messages
	 */
	public final static int REQUEST_TIMEOUT = 2000;
	/**
	 * Amount of times the session retries subscribing
	 */
	public final static int RETRY_COUNT = 10;
	/**
	 * Protobuf message used to create this session
	 */
	private Session session;
	private boolean active = true;

	/**
	 * Creating a new service session
	 * @param request	Protobuf Session message
	 * @param factory	ConnectionFactory that is used for managing pub/sub handlers
	 */
	public ServiceSession(Session request, ConnectionFactory factory) {
		this.session = request;
		this.factory = factory;
		subscribeHandler = factory.newSubscribeHandler(this);
		publishHandler = factory.newPublishHandler(this);
		
		setupZMQ(request.getPort(), request.getAddress());
	}
	
	/**
	 * Setup ZeroMQ context and sockets
	 * @param bindPort			The port the program will bind for subscribing
	 * @param connectAddress	The address of the remote service for publishing
	 */
	private void setupZMQ(int bindPort, String connectAddress){
		ctx = ZMQ.context(1);
		subscribeSocket = ctx.socket(ZMQ.SUB);
		publishSocket = ctx.socket(ZMQ.PUB);
		publishSocket.setSendTimeOut(REQUEST_TIMEOUT);
		subscribeSocket.bind("tcp://*:" + bindPort);
		publishSocket.connect(connectAddress);
		subscribeSocket.subscribe("".getBytes());
	}

	/**
	 * Start consumption thread
	 */
	public void startConsuming() {
		if (subscribeHandler != null) {
			new SubscribeThread().start();
		}
	}

	/**
	 * Publish raw serialized byte array
	 * @param body	Byte array message
	 * @return		Whether the publish was successful
	 */
	public boolean publish(byte[] body) {
		if (publishSocket != null) {
			return publishSocket.send(body);
		}
		return false;
	}

	/**
	 * Stop the current session and delete references and close sockets
	 */
	public void stopSession() {
		active = false;
		factory.deleteSubscribeHandler(subscribeHandler);
		factory.deletePublishHandler(publishHandler);
		subscribeHandler.deactivate();
		publishHandler.deactivate();
		subscribeHandler = null;
		publishHandler = null;
		subscribeSocket.close();
		publishSocket.close();
		ctx.close();
	}

	public SubscribeHandler<?> getSubscribeHandler() {
		return subscribeHandler;
	}

	public PublishHandler<?> getPublishHandler() {
		return publishHandler;
	}
	public Session getSession() {
		return session;
	}
	public void setSession(Session session) {
		this.session = session;
	}

	/**
	 * @author Maarten Kollenstart
	 * Loop that will collect raw messages that will be forwarded to the subscribeHandler
	 */
	class SubscribeThread extends Thread {
		private int fails = 0;
		public void run() {
			while (active && fails < RETRY_COUNT) {
				try {
					subscribeHandler.receiveMessage(subscribeSocket.recv());
					fails = 0;
				} catch (ZMQException e) {
					fails++;
					System.out.println("Subscribe error: "+ZError.toString(e.getErrorCode()));
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
			}
		}
	}
}
