/**
 * File OrchestratorApplication.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.rest;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.glassfish.jersey.jackson.JacksonFeature;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;

/**
 * OrchestratorApplication
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 20 mrt. 2017
 */
public class OrchestratorApplication extends Application {

    public OrchestratorApplication() {
        final BeanConfig beanConfig = new BeanConfig();
        beanConfig.setResourcePackage("org.flexiblepower");
        beanConfig.setScan(true);
        beanConfig.setPrettyPrint(true);
    }

    @Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> resources = new HashSet<>();

        resources.add(NodeApi.class);
        resources.add(ConnectionApi.class);
        resources.add(ApiListingResource.class);
        resources.add(SwaggerSerializers.class);
        resources.add(JacksonFeature.class);
        resources.add(CORSResponseFilter.class);

        return resources;
    }

}
