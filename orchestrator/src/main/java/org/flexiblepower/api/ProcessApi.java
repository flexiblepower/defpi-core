package org.flexiblepower.api;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.Api;

@Path("process")
@Api("Process")
@Produces(MediaType.APPLICATION_JSON)
public interface ProcessApi {

    @GET
    public List<Process> listProcesses();

    @GET
    @Path("{uuid}")
    public Process getProcess(@PathParam("uuid") final String uuid);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public String newProcess(final String json);
}