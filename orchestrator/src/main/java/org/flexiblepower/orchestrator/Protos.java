package org.flexiblepower.orchestrator;

import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Protos {

	final static Logger logger = LoggerFactory.getLogger(Containers.class);
	private MongoDbConnector d;
	
	public Protos(ObjectId user){
		d = new MongoDbConnector();
		d.setApplicationUser(user);
	}
	public Protos(){
		d = new MongoDbConnector();
	}
	
	public List<Document> getProtos(){
		return d.getProtos();
	}
	public Document getProto(String sha256) {
		return d.getProto(sha256);
	}
	public void insertProto(String name, String sha256, String proto) {
		if(getProto(sha256) == null){
			d.insertProto(name, sha256, proto);
		}
	}
	public void deleteProto(String sha256) {
		d.deleteProto(sha256);
	}
}
