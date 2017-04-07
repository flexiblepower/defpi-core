package org.flexiblepower.api;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.flexiblepower.model.Service;

import io.swagger.annotations.Api;

@Api("Service")
@Path("service")
@Produces(MediaType.APPLICATION_JSON)
public interface ServiceApi {

    @GET
    public List<String> listServices();

    @GET
    @Path("{image}/{tag}")
    public Service listService(@PathParam("image") final String imageName, @PathParam("tag") final String tag);

    @DELETE
    @Path("{image}/{tag}")
    public void deleteService(@PathParam("image") final String image, @PathParam("tag") final String tag);

}
