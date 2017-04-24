package org.flexiblepower.service;

import org.flexiblepower.proto.ServiceProto.ConnectionMessage;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQException;

import zmq.ZError;

public class ServiceSession {

    /**
     * ConnectionFactory that is used to retrieve manage subscribe and publish handlers
     */
    private final ConnectionFactory factory;
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
    private ConnectionMessage session;
    private boolean active = true;

    /**
     * Creating a new service session
     *
     * @param request Protobuf Session message
     * @param factory ConnectionFactory that is used for managing pub/sub handlers
     */
    public ServiceSession(final ConnectionMessage request, final ConnectionFactory factory) {
        this.session = request;
        this.factory = factory;
        this.subscribeHandler = factory.newSubscribeHandler(this);
        this.publishHandler = factory.newPublishHandler(this);

        this.setupZMQ(request.getListenPort(), request.getTargetAddress());
    }

    /**
     * Setup ZeroMQ context and sockets
     *
     * @param bindPort The port the program will bind for subscribing
     * @param connectAddress The address of the remote service for publishing
     */
    private void setupZMQ(final int bindPort, final String connectAddress) {
        this.ctx = ZMQ.context(1);
        this.subscribeSocket = this.ctx.socket(ZMQ.SUB);
        this.publishSocket = this.ctx.socket(ZMQ.PUB);
        this.publishSocket.setSendTimeOut(ServiceSession.REQUEST_TIMEOUT);
        this.subscribeSocket.bind("tcp://*:" + bindPort);
        this.publishSocket.connect(connectAddress);
        this.subscribeSocket.subscribe("".getBytes());
    }

    /**
     * Start consumption thread
     */
    public void startConsuming() {
        if (this.subscribeHandler != null) {
            new SubscribeThread().start();
        }
    }

    /**
     * Publish raw serialized byte array
     *
     * @param body Byte array message
     * @return Whether the publish was successful
     */
    public boolean publish(final byte[] body) {
        if (this.publishSocket != null) {
            return this.publishSocket.send(body);
        }
        return false;
    }

    /**
     * Stop the current session and delete references and close sockets
     */
    public void stopSession() {
        this.active = false;
        this.factory.deleteSubscribeHandler(this.subscribeHandler);
        this.factory.deletePublishHandler(this.publishHandler);
        this.subscribeHandler.deactivate();
        this.publishHandler.deactivate();
        this.subscribeHandler = null;
        this.publishHandler = null;
        this.subscribeSocket.close();
        this.publishSocket.close();
        this.ctx.close();
    }

    public SubscribeHandler<?> getSubscribeHandler() {
        return this.subscribeHandler;
    }

    public PublishHandler<?> getPublishHandler() {
        return this.publishHandler;
    }

    public ConnectionMessage getSession() {
        return this.session;
    }

    public void setSession(final ConnectionMessage session) {
        this.session = session;
    }

    /**
     * @author Maarten Kollenstart
     *         Loop that will collect raw messages that will be forwarded to the subscribeHandler
     */
    class SubscribeThread extends Thread {

        private int fails = 0;

        @Override
        public void run() {
            while (ServiceSession.this.active && (this.fails < ServiceSession.RETRY_COUNT)) {
                try {
                    ServiceSession.this.subscribeHandler.receiveMessage(ServiceSession.this.subscribeSocket.recv());
                    this.fails = 0;
                } catch (final ZMQException e) {
                    this.fails++;
                    System.out.println("Subscribe error: " + ZError.toString(e.getErrorCode()));
                    try {
                        Thread.sleep(1000);
                    } catch (final InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
    }
}
