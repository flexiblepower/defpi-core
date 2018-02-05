/**
 * File RegistryConnectorTest.java
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
package org.flexiblepower.orchestrator;

import java.util.Collection;

import org.flexiblepower.connectors.RegistryConnector;
import org.flexiblepower.exceptions.RepositoryNotFoundException;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.Service;
import org.junit.Assume;
import org.junit.Test;

/**
 * DockerConnectorTest
 *
 * @version 0.1
 * @since May 8, 2017
 */
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
        System.out.format("Found %d services\n", services.size());

        if (!services.isEmpty()) {
            System.out.println(services.iterator().next().getInterfaces());
        }

        Thread.sleep(5000);
        final Collection<Service> services2 = this.connector.getServices(this.repository);
        System.out.format("Found %d services\n", services2.size());

        System.out.println(this.connector.getAllServiceVersions(this.repository));

        System.out.println(this.connector.getService(this.repository, "observations"));
    }
}
