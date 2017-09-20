/**
 * File DockerConnectorTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator;

import org.flexiblepower.connectors.RegistryConnector;
import org.flexiblepower.exceptions.RepositoryNotFoundException;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.junit.Test;

/**
 * DockerConnectorTest
 *
 * @author coenvl
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
        System.out.println(this.connector.listServiceVersions(this.repository));
    }

    @Test
    public void testGetService() throws RepositoryNotFoundException, ServiceNotFoundException {
        System.out.println(this.connector.getService(this.repository, "observations:latest"));
    }
}
