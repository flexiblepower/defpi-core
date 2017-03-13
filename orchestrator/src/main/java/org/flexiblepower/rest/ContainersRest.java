package org.flexiblepower.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import org.flexiblepower.gson.ContainerDescription;
import org.flexiblepower.gson.InitGson;
import org.flexiblepower.orchestrator.Containers;
import org.flexiblepower.orchestrator.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

@Path("containers")
public class ContainersRest {
	public final static Logger logger = LoggerFactory.getLogger(ContainersRest.class);
	Containers containers;

	private boolean initUser(HttpHeaders httpHeaders){
		ObjectId userId = new User().getUserId(httpHeaders);
		if(userId != null){
			containers = new Containers(userId);
			return true;
		}
		return false;
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response listContainers(@Context HttpHeaders httpHeaders) {
		logger.info("REST");
		if (initUser(httpHeaders)) {
			return Response.ok(new Document("containers", containers.listContainers()).toJson()).build();
		}
		return Response.status(Status.UNAUTHORIZED).build();
	}

	@GET
	@Path("{uuid}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response listContainer(@Context HttpHeaders httpHeaders, @PathParam("uuid") String uuid) {
		if (initUser(httpHeaders)) {
			return Response.ok(containers.getContainer(uuid).toJson()).build();
		}
		return Response.status(Status.UNAUTHORIZED).build();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response newContainer(@Context HttpHeaders httpHeaders, String json) {
		logger.info("newContainer(): " + json);
		if (initUser(httpHeaders)) {
			try {
				Gson gson = InitGson.create();
				ContainerDescription containerDescription = gson.fromJson(json, ContainerDescription.class);
				if(containers.createContainer(containerDescription) == null){
					return Response.status(Status.FORBIDDEN).build();
				}
			} catch (Exception e) {
				logger.error(e.toString());
				return Response.status(Status.BAD_REQUEST).build();
			}
			return Response.ok().build();
		}
		return Response.status(Status.UNAUTHORIZED).build();
	}

	@DELETE
	@Path("{uuid}")
	public Response deleteContainer(@Context HttpHeaders httpHeaders, @PathParam("uuid") String uuid) {
		logger.info("deleteContainer('" + uuid + "')");
		if (initUser(httpHeaders)) {
			return Response.status(containers.deleteContainer(uuid)).build();
		}
		return Response.status(Status.UNAUTHORIZED).build();
	}

	@POST
	@Path("{uuid}/upgrade")
	public Response upgrade(@Context HttpHeaders httpHeaders, @PathParam("uuid") String uuid) {
		logger.info("Upgrade: " + uuid);
		if (initUser(httpHeaders)) {
			Status status = containers.upgradeContainer(containers.getContainer(uuid));
			return Response.status(status).build();
		}
		return Response.status(Status.UNAUTHORIZED).build();
	}
}