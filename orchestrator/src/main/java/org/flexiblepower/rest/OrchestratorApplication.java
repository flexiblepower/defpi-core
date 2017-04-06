/**
 * File OrchestratorApplication.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.rest;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.jackson.JacksonFeature;

import io.swagger.annotations.Contact;
import io.swagger.annotations.ExternalDocs;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;
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
@SwaggerDefinition(info = @Info(description = "API for communicating with the distributed EF-Pi orchestrator",
                                version = "V0.1",
                                title = "dEF-Pi API",
                                termsOfService = "Proof of concept, for testing purposes only. No rights or claims can be derived from this",
                                contact = @Contact(name = "TNO",
                                                   email = "coen.vanleeuwen@tno.nl",
                                                   url = "http://www.tno.nl"),
                                license = @License(name = "Apache 2.0", url = "http://www.apache.org")),
                   consumes = {MediaType.APPLICATION_JSON},
                   produces = {MediaType.APPLICATION_JSON},
                   externalDocs = @ExternalDocs(value = "Flexiblepower.io", url = "http://flexiblepower.github.io/"))
public class OrchestratorApplication extends Application {

    public OrchestratorApplication(final URI publishURI) {
        final BeanConfig beanConfig = new BeanConfig();
        beanConfig.setResourcePackage("org.flexiblepower");
        beanConfig.setSchemes(new String[] {publishURI.getScheme()});
        beanConfig.setHost(publishURI.getHost() + (publishURI.getPort() == 80 ? "" : ":" + publishURI.getPort()));
        beanConfig.setBasePath(publishURI.getPath());
        beanConfig.setScan(true);
        beanConfig.setPrettyPrint(true);
    }

    @Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> resources = new HashSet<>();

        // Add resources for the model
        resources.add(ConnectionApi.class);
        resources.add(InterfaceApi.class);
        resources.add(NodeApi.class);
        resources.add(ProcessApi.class);
        resources.add(ServiceApi.class);
        resources.add(UserApi.class);

        resources.add(ExceptionMapper.class);

        // Add resources for swagger, jackson and CORS
        resources.add(ApiListingResource.class);
        resources.add(SwaggerSerializers.class);
        resources.add(JacksonFeature.class);
        resources.add(CORSResponseFilter.class);

        return resources;
    }

}
