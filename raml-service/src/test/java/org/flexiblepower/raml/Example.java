/**
 * File Example.java
 *
 * Copyright 2019 FAN
 */
package org.flexiblepower.raml;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

/**
 * Example
 *
 * @version 0.1
 * @since Aug 27, 2019
 */
@Path("/example")
@SuppressWarnings("javadoc")
public interface Example {

    @GET
    public String getExampleText();

    @GET
    public String getPersonalText(@QueryParam("name") final String name);

    @GET
    @Path("{reps}")
    public String getPersonalText(@PathParam("reps") final int reps);

    @POST
    @Path("complicated/{id}")
    public float setStuff(@PathParam("id") final int id,
            @QueryParam("q") final String q,
            @QueryParam("test") double test,
            Map<String, String> body);

}
