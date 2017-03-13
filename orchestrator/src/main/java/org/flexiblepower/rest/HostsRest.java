package org.flexiblepower.rest;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.flexiblepower.orchestrator.Database;
import org.flexiblepower.orchestrator.Hosts;
import org.flexiblepower.orchestrator.Swarm;
import org.flexiblepower.orchestrator.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("hosts")
public class HostsRest {
	final static Logger logger = LoggerFactory.getLogger(HostsRest.class);
	Database d = new Database();
	Hosts hosts;
	ObjectId userId;
	
	private boolean initUser(HttpHeaders httpHeaders){
		userId = new User().getUserId(httpHeaders);
		if(userId != null){
			hosts = new Hosts(userId);
			return true;
		}
		return false;
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response listHosts(@Context HttpHeaders httpHeaders) {
		if(initUser(httpHeaders)){
			return Response.ok(new Document("hosts", hosts.getHosts()).toJson()).build();
		}
		return Response.status(Status.UNAUTHORIZED).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("new")
	public Response newHost(@javax.ws.rs.core.Context HttpHeaders httpHeaders){
		if(initUser(httpHeaders)){
			String command = Swarm.newHost();
			String publicCommand = command.replace("sudo docker run", "sudo docker run -e CATTLE_HOST_LABELS='type=public'");
			String privateCommand = command.replace("sudo docker run", "sudo docker run -e CATTLE_HOST_LABELS='type=private&user="+userId+"'");;
			return Response.ok("{\"publicCommand\":\""+publicCommand+"\", \"privateCommand\": \""+privateCommand+"\"}").build();
		}
		return Response.status(Status.UNAUTHORIZED).build();
	}
	
	@POST
	@Path("{action}/{host}")
	public Response hostAction(@javax.ws.rs.core.Context HttpHeaders httpHeaders, @PathParam("action") String action, @PathParam("host") String host){
		logger.info("Host action: "+action+", "+host);
		if(initUser(httpHeaders)){
			int status = Swarm.hostAction(host, action);
			logger.info("Rancher status: "+ status);
			Swarm.syncHosts();
			return Response.status(status).build();
		}
		return Response.status(Status.UNAUTHORIZED).build();
	}
}
