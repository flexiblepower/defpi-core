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
    public List<String> listRepositories();

    @GET
    @Path("{repository}")
    public List<String> listServices(@PathParam("repository") final String repository);

    @GET
    @Path("{repository}/{service}")
    public List<String> listTags(@PathParam("repository") final String repository,
            @PathParam("service") final String serviceName);

    @GET
    @Path("{repository}/{service}/{tag}")
    public Service getService(@PathParam("repository") final String repository,
            @PathParam("service") final String serviceName,
            @PathParam("tag") final String tag);

    @DELETE
    @Path("{repository}/{service}/{tag}")
    public void deleteService(@PathParam("repository") final String repository,
            @PathParam("service") final String serviceName,
            @PathParam("tag") final String tag);

}
