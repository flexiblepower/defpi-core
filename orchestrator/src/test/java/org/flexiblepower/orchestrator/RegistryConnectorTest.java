/**
 * File DockerConnectorTest.java
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

import org.flexiblepower.connectors.RegistryConnector;
import org.flexiblepower.exceptions.RepositoryNotFoundException;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.junit.Test;

/**
 * DockerConnectorTest
 *
 * @version 0.1
 * @since May 8, 2017
 */
public class RegistryConnectorTest {

    private final String repository = "services";

    private final RegistryConnector connector = RegistryConnector.getInstance();

    @Test
    public void testListServices() throws RepositoryNotFoundException, ServiceNotFoundException {
        System.out.println(this.connector.listServices(this.repository));
        System.out.println(this.connector.listServices(this.repository).get(0).getInterfaces());
    }

    @Test
    public void testListServiceVersions() throws RepositoryNotFoundException, ServiceNotFoundException {
        System.out.println(this.connector.listAllServiceVersions(this.repository));
    }

    @Test
    public void testGetService() throws RepositoryNotFoundException, ServiceNotFoundException {
        System.out.println(this.connector.getService(this.repository, "observations"));
    }
}
