package org.flexiblepower.orchestrator;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.ws.rs.core.Response.Status;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.flexiblepower.model.Connection;
import org.flexiblepower.protos.SessionProto.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

public class Links {
	final static Logger logger = LoggerFactory.getLogger(Containers.class);
	private MongoDbConnector d;

	public Links(ObjectId user) {
		d = new MongoDbConnector();
		d.setApplicationUser(user);
	}

	public Links() {
		d = new MongoDbConnector();
	}

	public List<Document> getLinks() {
		return d.getLinks();
	}

	public Status newLink(Connection link) {
		logger.info("Check if link is allowed");
		if (d.linkAllowed(link)) {
			logger.info("Link is allowed");
			Document c1 = d.getContainer(link.getContainer1());
			Document c2 = d.getContainer(link.getContainer2());

			int port1 = 5000 + new Random().nextInt(5000);
			int port2 = 5000 + new Random().nextInt(5000);
			String address1 = "tcp://" + c1.getString("ip") + ":" + port1;
			String address2 = "tcp://" + c2.getString("ip") + ":" + port2;

			// Add reference
			UUID id = UUID.randomUUID();
			link.setId(id.toString());
			d.insertLink(link);

			Session s1 = Session.newBuilder().setId(id.toString()).setMode(Session.ModeType.CREATE).setAddress(address2)
					.setPort(port1).setSubscribeHash(link.getInterface1()).setPublishHash(link.getInterface2()).build();
			Session s2 = Session.newBuilder().setId(id.toString()).setMode(Session.ModeType.CREATE).setAddress(address1)
					.setPort(port2).setSubscribeHash(link.getInterface2()).setPublishHash(link.getInterface1()).build();

			logger.info("Sending session to tcp://" + c1.getString("ip") + ":4999");
			int response1 = sendMessage(c1.getString("ip"), s1);
			logger.info("Sending session to tcp://" + c2.getString("ip") + ":4999");
			int response2 = sendMessage(c2.getString("ip"), s2);
			if(response1 != 0 || response2 != 0){
				logger.info("LINK error: "+response1+", "+response2);
				return Status.INTERNAL_SERVER_ERROR;
			}
			logger.info("Done sending");
			return Status.OK;
		}
		return Status.FORBIDDEN;
	}

	public Status deleteLink(String id) {
		Document link = d.getLink(id);
		if (link != null) {
			d.deleteLink(id);
			final Session session = Session.newBuilder().setId(id.toString()).setMode(Session.ModeType.DELETE).build();
			final Document c1 = d.getContainer(link.getString("container1"));
			final Document c2 = d.getContainer(link.getString("container2"));
			(new Thread() {
				public void run() {
					if (c1 != null) {
						logger.info("Sending session to tcp://" + c1.getString("ip") + ":4999");
						sendMessage(c1.getString("ip"), session);
					}
					if (c2 != null) {
						logger.info("Sending session to tcp://" + c2.getString("ip") + ":4999");
						sendMessage(c2.getString("ip"), session);
					}
					logger.info("Done sending");
				}
			}).start();
			return Status.OK;
		}
		return Status.ACCEPTED;
	}

	private int sendMessage(String ip, Session session){
		return sendMessage(ip, session, 10);
	}
	
	private int sendMessage(String ip, Session session, int retry) {
		Context ctx = ZMQ.context(1);
		Socket socket = ctx.socket(ZMQ.REQ);
		logger.info("Connecting to socket");
		socket.connect("tcp://" + ip + ":4999");
		logger.info("Sending data");
		socket.send(session.toByteArray(), 0);
		int response = socket.recv()[0];
		logger.info("Response: "+response);
		if (response != 0) {
			if(retry != 0){
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				logger.info("Retry");
				sendMessage(ip, session, retry - 1);
			}
			return response;
		}
		socket.close();
		ctx.close();
		return response;
	}

	public MongoDbConnector getDatabase() {
		return d;
	}
}
