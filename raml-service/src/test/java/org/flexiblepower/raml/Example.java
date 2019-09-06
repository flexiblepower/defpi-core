/*-
 * #%L
 * dEF-Pi RAML services
 * %%
 * Copyright (C) 2017 - 2019 Flexible Power Alliance Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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
