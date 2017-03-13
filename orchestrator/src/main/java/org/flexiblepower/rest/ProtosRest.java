package org.flexiblepower.rest;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

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
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.flexiblepower.orchestrator.Protos;
import org.flexiblepower.orchestrator.User;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.hash.Hashing;

@Path("protos")
public class ProtosRest {
	final static Logger logger = LoggerFactory.getLogger(ProtosRest.class);
	private Protos protos;

	private boolean initUser(HttpHeaders httpHeaders) {
		ObjectId userId = new User().getUserId(httpHeaders);
		if (userId != null) {
			protos = new Protos(userId);
			return true;
		}
		return false;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response listProtos(@Context HttpHeaders httpHeaders) {
		if (initUser(httpHeaders)) {
			return Response.ok(new Document("protos", protos.getProtos()).toJson()).build();
		}
		return Response.status(Status.UNAUTHORIZED).build();
	}

	@GET
	@Path("{sha256}")
	@Produces(MediaType.TEXT_PLAIN)
	public Response download(@Context HttpHeaders httpHeaders, @PathParam("sha256") String sha256) {
		protos = new Protos(null);
		Document s = protos.getProto(sha256);
		if (s == null)
			return Response.status(Status.NOT_FOUND).build();
		ResponseBuilder response = Response.ok(s.getString("proto"));
		response.header("Content-Disposition", "attachment; filename=\"message.proto\"");
		return response.build();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response newProto(@Context HttpHeaders httpHeaders, String json) throws NoSuchAlgorithmException {
		protos = new Protos(null);
		JSONObject o = new JSONObject(json);
		String sha256 = "";
		if (o.has("sha256")) {
			sha256 = o.getString("sha256");
		} else {
			sha256 = Hashing.sha256().hashString(o.getString("proto"), StandardCharsets.UTF_8).toString();
		}
		protos.insertProto(o.getString("name"), sha256, o.getString("proto"));
		return Response.ok().build();
	}

	@DELETE
	@Path("{sha256}")
	public Response deleteProto(@Context HttpHeaders httpHeaders, @PathParam("sha256") String sha256) {
		if (initUser(httpHeaders)) {
			protos.deleteProto(sha256);
			return Response.ok().build();
		}
		return Response.status(Status.UNAUTHORIZED).build();
	}
}
