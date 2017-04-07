package org.flexiblepower.api;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.flexiblepower.model.Interface;

import io.swagger.annotations.Api;

@Api("Interface")
@Path("interface")
@Produces(MediaType.APPLICATION_JSON)
public interface InterfaceApi {

    @GET
    public List<Interface> listProtos();

    @GET
    @Path("{sha256}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response download(@PathParam("sha256") final String sha256);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public String newInterface(final Interface itf);

    @DELETE
    @Path("{sha256}")
    public void deleteInterface(@PathParam("sha256") final String sha256);
}
