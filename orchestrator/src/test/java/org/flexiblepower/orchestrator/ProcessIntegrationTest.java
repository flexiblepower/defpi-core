/**
 * File ProcessIntegrationTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator;

import java.util.HashSet;
import java.util.Set;

import org.flexiblepower.exceptions.RepositoryNotFoundException;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.PrivateNode;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.Service;
import org.junit.Test;

/**
 * ProcessIntegrationTest
 *
 * @author coenvl
 * @version 0.1
 * @since Apr 24, 2017
 */
public class ProcessIntegrationTest {

    private final DockerConnector connector = new DockerConnector();

    private final RegistryConnector registry = new RegistryConnector();

    @Test
    public void listServices() throws RepositoryNotFoundException {
        System.out.println(this.registry.listServices("defpi"));
    }

    @Test
    public void runTest() throws ServiceNotFoundException {
        final Service service = this.registry.getService("services", "echo", "0.0.1");
        final Set<String> ports = new HashSet<>();
        ports.add("4999:4999");
        service.setPorts(ports);

        final Process process = new Process();
        process.setProcessService(service);
        process.setUserName("TestUser");
        process.setRunningNode(new PrivateNode("ubuntu", "TestUser"));

        this.connector.newProcess(process);
    }

}
