package org.flexiblepower.orchestrator;

import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hosts {
	final static Logger logger = LoggerFactory.getLogger(Hosts.class);
	private MongoDbConnector d;
	
	public Hosts(ObjectId user){
		d = new MongoDbConnector();
		d.setApplicationUser(user);
	}
	public Hosts(){
		d = new MongoDbConnector();
	}
	
	public List<Document> getHosts(){
		return d.getHosts();
	}
}
