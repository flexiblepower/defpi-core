package org.flexiblepower.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.flexiblepower.gson.InitGson;
import org.flexiblepower.gson.Link;
import org.flexiblepower.orchestrator.Links;
import org.flexiblepower.orchestrator.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

@Path("links")
public class LinksRest {
	final static Logger logger = LoggerFactory.getLogger(LinksRest.class);
	Links links;
	private boolean initUser(HttpHeaders httpHeaders){
		ObjectId userId = new User().getUserId(httpHeaders);
		if(userId != null){
			links = new Links(userId);
			return true;
		}
		return false;
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response listLinks(@javax.ws.rs.core.Context HttpHeaders httpHeaders) {
		if(initUser(httpHeaders)){
			return Response.ok(new Document("links", links.getLinks()).toJson()).build();
		}
		return Response.status(Status.UNAUTHORIZED).build();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response newLink(@javax.ws.rs.core.Context HttpHeaders httpHeaders, String json) {
		logger.info("newLink(): " + json);
		if(initUser(httpHeaders)){
			try{
				Gson gson = InitGson.create();
				Link link = gson.fromJson(json, Link.class);
				return Response.status(links.newLink(link)).build();
			} catch (JsonSyntaxException e){
				logger.info("Parse exception: "+e);
				return Response.status(Status.BAD_REQUEST).build();
			}
		}
		return Response.status(Status.UNAUTHORIZED).build();
	}

	@DELETE
	@Path("{id}")
	public Response deleteLink(@javax.ws.rs.core.Context HttpHeaders httpHeaders, @PathParam("id") String id) {
		logger.info("deleteLink(): " + id);
		if(initUser(httpHeaders)){
			return Response.status(links.deleteLink(id)).build();
		}
		return Response.status(Status.UNAUTHORIZED).build();
	}
}
