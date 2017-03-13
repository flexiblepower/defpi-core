package org.flexiblepower.orchestrator;

import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.glassfish.jersey.internal.util.Base64;

public class User {
	private final Database db = new Database();
	private Document user = null;
	
	public Document getUser(HttpHeaders httpHeaders){
		MultivaluedMap<String, String> headers = httpHeaders.getRequestHeaders();
		if(headers.containsKey("authorization")){
			List<String> auths = headers.get("authorization");
			byte[] auth = auths.get(0).substring(6).getBytes();
			String[] credentials = new String(Base64.decode(auth)).split(":");
			user = db.getUser(credentials[0], credentials[1]);
			return user;
		}
		return null;
	}
	
	public ObjectId getUserId(HttpHeaders httpHeaders){
		if(getUser(httpHeaders) == null) return null;
		
		return user.getObjectId("_id");
	}
	
	public void addUser(String username, String password){
		db.insertUser(username, password);
	}
	
}
