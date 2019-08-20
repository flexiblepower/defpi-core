/**
 * File TestConnectionHandler.java
 *
 * Copyright 2019 FAN
 */
package org.flexiblepower.raml;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.flexiblepower.service.ConnectionHandler;

/**
 * TestConnectionHandler
 *
 * @version 0.1
 * @since Aug 20, 2019
 */
public class TestConnectionHandler implements ConnectionHandler {

    @Path("/example")
    public interface Example {

        @GET
        public String getExampleText();

        @GET
        public String getPersonalText(@QueryParam("name") final String name);

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
