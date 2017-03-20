package org.flexiblepower.rest;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
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
import org.flexiblepower.model.User;
import org.flexiblepower.orchestrator.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("services")
public class ServicesRest {
	public final static Logger logger = LoggerFactory.getLogger(ServicesRest.class);

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response listServices(@Context HttpHeaders httpHeaders) {
		ObjectId user = new User().getUserId(httpHeaders);
		if (user != null) {
			Services services = new Services(user);
			return Response.ok(new Document("services", services.listServices()).toJson()).build();
		}
		return Response.status(Status.UNAUTHORIZED).build();
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{image}/{tag}")
	public Response listService(@Context HttpHeaders httpHeaders, @PathParam("image") String image, @PathParam("tag") String tag) {
		ObjectId user = new User().getUserId(httpHeaders);
		if (user != null) {
			Services services = new Services(user);
			return Response.ok(new Document("services", services.listServices()).toJson()).build();
		}
		return Response.status(Status.UNAUTHORIZED).build();
	}
	
	@DELETE
	@Path("{image}/{tag}")
	public Response deleteService(@Context HttpHeaders httpHeaders, @PathParam("image") String image, @PathParam("tag") String tag){
		logger.info("Deleting "+image+" : "+tag);
		ObjectId user = new User().getUserId(httpHeaders);
		if (user != null) {
			Services services = new Services(user);
			services.deleteService(image, tag);
		}
		return Response.ok().build();
	}

}
