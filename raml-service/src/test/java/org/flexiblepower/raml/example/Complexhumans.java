package org.flexiblepower.raml.example;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.flexiblepower.raml.example.model.Human;
import org.flexiblepower.raml.example.model.HumanId;
import org.flexiblepower.raml.example.support.ResponseDelegate;

@Path("/complexhumans")
public interface Complexhumans {
  @GET
  @Produces("application/json")
  GetComplexhumansResponse getComplexhumans(@QueryParam("id") HumanId id);

  @GET
  @Path("/{id}")
  @Produces("application/json")
  GetComplexhumansByIdResponse getComplexhumansById(@PathParam("id") HumanId id);

  class GetComplexhumansResponse extends ResponseDelegate {
    private GetComplexhumansResponse(Response response, Object entity) {
      super(response, entity);
    }

    private GetComplexhumansResponse(Response response) {
      super(response);
    }

    public static GetComplexhumansResponse respond200WithApplicationJson(Human entity) {
      Response.ResponseBuilder responseBuilder = Response.status(200).header("Content-Type", "application/json");
      responseBuilder.entity(entity);
      return new GetComplexhumansResponse(responseBuilder.build(), entity);
    }
  }

  class GetComplexhumansByIdResponse extends ResponseDelegate {
    private GetComplexhumansByIdResponse(Response response, Object entity) {
      super(response, entity);
    }

    private GetComplexhumansByIdResponse(Response response) {
      super(response);
    }

    public static GetComplexhumansByIdResponse respond200WithApplicationJson(Human entity) {
      Response.ResponseBuilder responseBuilder = Response.status(200).header("Content-Type", "application/json");
      responseBuilder.entity(entity);
      return new GetComplexhumansByIdResponse(responseBuilder.build(), entity);
    }
  }
}
