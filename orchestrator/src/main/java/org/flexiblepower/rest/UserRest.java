package org.flexiblepower.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.flexiblepower.model.User;
import org.flexiblepower.orchestrator.MongoDbConnector;

@Path("user")
public class UserRest {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addUser(final User user) {
        final MongoDbConnector db = new MongoDbConnector();
        db.addUser(user);
        return Response.ok().build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public User testGetUser() {
        return new User("test", "test");
    }
}
