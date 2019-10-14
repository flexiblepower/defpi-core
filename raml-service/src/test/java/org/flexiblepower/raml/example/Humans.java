package org.flexiblepower.raml.example;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.flexiblepower.raml.example.model.Human;
import org.flexiblepower.raml.example.model.Person;

@Path("/humans")
public interface Humans {
  @GET
  @Produces("application/json")
  List<Human> getHumans(@QueryParam("type") String type);

  @GET
  @Path("/all")
  @Produces("application/json")
  List<Human> getHumansAll();

  @GET
  @Path("/{id}")
  @Produces("application/json")
  Human getHumansById(@PathParam("id") String id, @QueryParam("userType") String userType);

  @PUT
  @Path("/{id}")
  @Consumes("application/json")
  void putHumansById(@PathParam("id") String id, Human entity);

  @GET
  @Path("/person/{id}")
  @Produces("application/json")
  Person getHumansPersonById(@PathParam("id") String id, @QueryParam("type") String type);
}
