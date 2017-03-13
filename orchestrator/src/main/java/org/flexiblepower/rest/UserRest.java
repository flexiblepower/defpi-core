package org.flexiblepower.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.flexiblepower.orchestrator.User;
import org.json.JSONObject;

@Path("user")
public class UserRest {

	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addUser(String json){
		User u = new User();
		JSONObject newUser = new JSONObject(json);
		u.addUser(newUser.getString("username"), newUser.getString("password"));
		return Response.ok().build();
	}
}
