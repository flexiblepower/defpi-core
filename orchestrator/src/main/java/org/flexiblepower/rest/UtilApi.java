/*-
 * #%L
 * dEF-Pi REST Orchestrator
 * %%
 * Copyright (C) 2017 - 2018 Flexible Power Alliance Network
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
package org.flexiblepower.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.flexiblepower.connectors.DockerConnector;
import org.flexiblepower.exceptions.AuthorizationException;

/**
 * UtilApi contains some utilities for the running server. These are not published via swagger or the WADL, but are
 * only intended for the server administrators. However these functions are openly available so care should be taken as
 * to what information is available here.
 *
 * @author coenvl
 * @version 0.1
 * @since Aug 31, 2017
 */
@Path("/")
@SuppressWarnings("static-method")
public class UtilApi extends BaseApi {

    /**
     * @param httpHeaders The headers from the HTTP request for authorization
     */
    protected UtilApi(@Context final HttpHeaders httpHeaders) {
        super(httpHeaders);
    }

    /**
     * @return A simple text message showing information about the current running image
     */
    @GET
    @Path("diag")
    @Produces(MediaType.TEXT_PLAIN)
    public String getVersion() {
        return DockerConnector.getInstance().getContainerInfo();
    }

    /**
     * @return A report of the current running services and the amount of instances. Great for viewing the global
     *         cluster health
     * @throws AuthorizationException When the user is not logged in as an admin user
     */
    @GET
    @Path("health")
    @Produces(MediaType.TEXT_PLAIN)
    public String getHealth() throws AuthorizationException {
        this.assertUserIsAdmin();
        return DockerConnector.getInstance().getServiceHealth();
    }

}
