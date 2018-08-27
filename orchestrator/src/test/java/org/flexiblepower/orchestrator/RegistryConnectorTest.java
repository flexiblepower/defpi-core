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
package org.flexiblepower.orchestrator;

import java.util.Collection;

import org.flexiblepower.connectors.RegistryConnector;
import org.flexiblepower.exceptions.RepositoryNotFoundException;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.Service;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

/**
 * DockerConnectorTest
 *
 * @version 0.1
 * @since May 8, 2017
 */
@Slf4j
@SuppressWarnings("javadoc")
public class RegistryConnectorTest {

    private final String repository = "services";

    private RegistryConnector connector;

    /**
     *
     */
    public RegistryConnectorTest() {
        try {
            this.connector = RegistryConnector.getInstance();
            this.connector.getServices(this.repository);
        } catch (final Exception e) {
            this.connector = null;
            Assume.assumeNoException(e);
        }
    }

    @Test(timeout = 15000)
    public void testListServices() throws RepositoryNotFoundException, ServiceNotFoundException, InterruptedException {
        final Collection<Service> services = this.connector.getServices(this.repository);
        Assert.assertNotNull(services);

        RegistryConnectorTest.log.debug("Found {} services", services.size());
        if (!services.isEmpty()) {
            RegistryConnectorTest.log.info("First service: {}", services.iterator().next().getInterfaces().toString());
        }

        Thread.sleep(5000);
        final Collection<Service> services2 = this.connector.getServices(this.repository);
        Assert.assertFalse(services2.isEmpty());
        System.out.format("Found %d services\n", services2.size());

        System.out.println(this.connector.getAllServiceVersions(this.repository));

        if (services2.size() > 0) {
            System.out.println(this.connector.getService(this.repository, "observations"));
        }
    }
}
