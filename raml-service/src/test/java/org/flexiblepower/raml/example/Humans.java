package org.flexiblepower.raml.example;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import org.flexiblepower.raml.example.model.Human;
import org.flexiblepower.raml.example.model.Person;
import org.flexiblepower.raml.example.support.ResponseDelegate;

@Path("/humans")
public interface Humans {
  @GET
  @Produces("application/json")
  GetHumansResponse getHumans(@QueryParam("type") String type);

  @GET
  @Path("/all")
  @Produces("application/json")
  GetHumansAllResponse getHumansAll();

  @GET
  @Path("/{id}")
  @Produces("application/json")
  GetHumansByIdResponse getHumansById(@PathParam("id") String id,
      @QueryParam("userType") String userType);

  @PUT
  @Path("/{id}")
  @Consumes("application/json")
  PutHumansByIdResponse putHumansById(@PathParam("id") String id, Human entity);

  @GET
  @Path("/person/{id}")
  @Produces("application/json")
  GetHumansPersonByIdResponse getHumansPersonById(@PathParam("id") String id,
      @QueryParam("type") String type);

  class GetHumansResponse extends ResponseDelegate {
    private GetHumansResponse(Response response, Object entity) {
      super(response, entity);
    }

    private GetHumansResponse(Response response) {
      super(response);
    }

    public static HeadersFor200 headersFor200() {
      return new HeadersFor200();
    }

    public static GetHumansResponse respond200WithApplicationJson(List<Human> entity,
        HeadersFor200 headers) {
      Response.ResponseBuilder responseBuilder = Response.status(200).header("Content-Type", "application/json");
      GenericEntity<List<Human>> wrappedEntity = new GenericEntity<List<Human>>(entity){};
      headers.toResponseBuilder(responseBuilder);
      responseBuilder.entity(wrappedEntity);
      return new GetHumansResponse(responseBuilder.build(), wrappedEntity);
    }

    public static class HeadersFor200 extends HeaderBuilderBase {
      private HeadersFor200() {
      }

      public HeadersFor200 withBoo(final String p) {
        headerMap.put("boo", String.valueOf(p));;
        return this;
      }

      public HeadersFor200 withSomethingElse(final String p) {
        headerMap.put("somethingElse", String.valueOf(p));;
        return this;
      }
    }
  }

  class GetHumansAllResponse extends ResponseDelegate {
    private GetHumansAllResponse(Response response, Object entity) {
      super(response, entity);
    }

    private GetHumansAllResponse(Response response) {
      super(response);
    }

    public static GetHumansAllResponse respond200WithApplicationJson(List<Human> entity) {
      Response.ResponseBuilder responseBuilder = Response.status(200).header("Content-Type", "application/json");
      GenericEntity<List<Human>> wrappedEntity = new GenericEntity<List<Human>>(entity){};
      responseBuilder.entity(wrappedEntity);
      return new GetHumansAllResponse(responseBuilder.build(), wrappedEntity);
    }
  }

  class PutHumansByIdResponse extends ResponseDelegate {
    private PutHumansByIdResponse(Response response, Object entity) {
      super(response, entity);
    }

    private PutHumansByIdResponse(Response response) {
      super(response);
    }

    public static HeadersFor200 headersFor200() {
      return new HeadersFor200();
    }

    public static PutHumansByIdResponse respond200(HeadersFor200 headers) {
      Response.ResponseBuilder responseBuilder = Response.status(200);
      responseBuilder = headers.toResponseBuilder(responseBuilder);
      return new PutHumansByIdResponse(responseBuilder.build());
    }

    public static class HeadersFor200 extends HeaderBuilderBase {
      private HeadersFor200() {
      }

      public HeadersFor200 withSomeOtherHeader(final String p) {
        headerMap.put("someOtherHeader", String.valueOf(p));;
        return this;
      }
    }
  }

  class GetHumansByIdResponse extends ResponseDelegate {
    private GetHumansByIdResponse(Response response, Object entity) {
      super(response, entity);
    }

    private GetHumansByIdResponse(Response response) {
      super(response);
    }

    public static GetHumansByIdResponse respond200WithApplicationJson(Human entity) {
      Response.ResponseBuilder responseBuilder = Response.status(200).header("Content-Type", "application/json");
      responseBuilder.entity(entity);
      return new GetHumansByIdResponse(responseBuilder.build(), entity);
    }
  }

  class GetHumansPersonByIdResponse extends ResponseDelegate {
    private GetHumansPersonByIdResponse(Response response, Object entity) {
      super(response, entity);
    }

    private GetHumansPersonByIdResponse(Response response) {
      super(response);
    }

    public static GetHumansPersonByIdResponse respond200WithApplicationJson(Person entity) {
      Response.ResponseBuilder responseBuilder = Response.status(200).header("Content-Type", "application/json");
      responseBuilder.entity(entity);
      return new GetHumansPersonByIdResponse(responseBuilder.build(), entity);
    }
  }
}
