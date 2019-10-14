package org.flexiblepower.raml.example;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.flexiblepower.raml.example.model.Human;
import org.flexiblepower.raml.example.model.HumanId;

@Path("/complexhumans")
public interface Complexhumans {
  @GET
  @Produces("application/json")
  Human getComplexhumans(@QueryParam("id") HumanId id);

  @GET
  @Path("/{id}")
  @Produces("application/json")
  Human getComplexhumansById(@PathParam("id") HumanId id);
}
