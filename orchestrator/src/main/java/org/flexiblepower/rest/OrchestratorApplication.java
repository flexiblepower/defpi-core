/**
 * File OrchestratorApplication.java
 *
 * Copyright 2017 FAN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flexiblepower.rest;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

import org.flexiblepower.api.OrchestratorApi;
import org.glassfish.jersey.jackson.JacksonFeature;

import io.swagger.annotations.BasicAuthDefinition;
import io.swagger.annotations.Contact;
import io.swagger.annotations.ExternalDocs;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SecurityDefinition;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;

/**
 * OrchestratorApplication
 *
 * @version 0.1
 * @since 20 mrt. 2017
 */
@SwaggerDefinition(info = @Info(description = "API for communicating with the distributed EF-Pi orchestrator",
                                version = "v1",
                                title = "dEF-Pi API",
                                termsOfService = "Licensed under the Apache License, Version 2.0. Copyright 2017 The Flexiblepower Alliance Network",
                                contact = @Contact(name = "TNO",
                                                   email = "coen.vanleeuwen@tno.nl",
                                                   url = "http://www.tno.nl"),
                                license = @License(name = "Apache 2.0", url = "http://www.apache.org")),
                   consumes = {MediaType.APPLICATION_JSON},
                   produces = {MediaType.APPLICATION_JSON},
                   externalDocs = @ExternalDocs(value = "dEF-Pi documentation on github",
                                                url = "https://github.com/flexiblepower/defpi-documentation"),
                   securityDefinition = @SecurityDefinition(basicAuthDefinitions = {
                           @BasicAuthDefinition(key = OrchestratorApi.USER_AUTHENTICATION,
                                                description = "This operation will only have effect for the logged in user."),
                           @BasicAuthDefinition(key = OrchestratorApi.ADMIN_AUTHENTICATION,
                                                description = "This operation can only be performed by an administrator.")}))
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
        resources.add(ConnectionRestApi.class);
        resources.add(NodeRestApi.class);
        resources.add(ProcessRestApi.class);
        resources.add(ServiceRestApi.class);
        resources.add(UserRestApi.class);
        resources.add(InterfaceRestApi.class);
        resources.add(PendingChangeRestApi.class);

        // Add some other APIs or resources for our backend
        resources.add(UtilApi.class);
        resources.add(ExceptionMapper.class);

        // Add resources for swagger, jackson and CORS
        resources.add(ApiListingResource.class);
        resources.add(SwaggerSerializers.class);
        resources.add(JacksonFeature.class);
        resources.add(CORSResponseFilter.class);

        return resources;
    }

}
