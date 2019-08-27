/*-
 * #%L
 * dEF-Pi API
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
package org.flexiblepower.raml.server;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.flexiblepower.service.ConnectionHandler;

/**
 * TestConnectionHandler
 *
 * @version 0.1
 * @since Aug 20, 2019
 */
@SuppressWarnings({"javadoc", "static-method"})
public class TestConnectionHandler implements ConnectionHandler {

    @Path("/example")
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

    public Example getExample() {
        return new Example() {

            @Override
            public String getExampleText() {
                return this.getPersonalText("world");
            }

            @Override
            public String getPersonalText(final String name) {
                return "Hello " + name + "!";
            }

            @Override
            public String getPersonalText(final int reps) {
                String ret = "";
                for (int i = 0; i < reps; i++) {
                    ret += this.getExampleText() + "\n";
                }
                return ret;
            }

            @Override
            public float setStuff(final int id, final String q, final double test, final Map<String, String> body) {
                return (float) (id + Double.parseDouble(body.get(q)) + Math.sqrt(test));
            }

        };
    }

    @Override
    public void onSuspend() {
        // TODO Auto-generated method stub
    }

    @Override
    public void resumeAfterSuspend() {
        // TODO Auto-generated method stub
    }

    @Override
    public void onInterrupt() {
        // TODO Auto-generated method stub
    }

    @Override
    public void resumeAfterInterrupt() {
        // TODO Auto-generated method stub
    }

    @Override
    public void terminated() {
        // TODO Auto-generated method stub
    }

}
