/**
 * File ProcessIntegrationTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.Connection;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.Service;
import org.flexiblepower.model.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;

import lombok.extern.slf4j.Slf4j;

/**
 * ProcessIntegrationTest
 *
 * @author coenvl
 * @version 0.1
 * @since Apr 24, 2017
 */
@Slf4j
public class ProcessIntegrationTest {

    private static final RegistryConnector registry = new RegistryConnector();
    private static final DockerConnector connector = new DockerConnector();
    private final ConnectionManager manager = new ConnectionManager();

    private static final String TEST_USER = "Maarten2";

    private static String processId1;
    private static String processId2;
    private static Service service;

    @BeforeClass
    public static void startClass() throws ServiceNotFoundException,
            DockerException,
            InterruptedException,
            DockerCertificateException,
            UnknownHostException {
        try {
            ProcessIntegrationTest.connector.newNetwork("user-net");
        } catch (final Exception e) {
            // Error creating network, probably already exists
        }

        final Service service = ProcessIntegrationTest.registry.getService("services", "echo", "0.0.1");

        final User user = new User(ProcessIntegrationTest.TEST_USER, UUID.randomUUID().toString());

        final String localhostName = InetAddress.getLocalHost().getHostName();

        ProcessIntegrationTest.service = service;
        ProcessIntegrationTest.processId1 = ProcessIntegrationTest.connector.newProcess(service, user, localhostName);
        ProcessIntegrationTest.processId2 = ProcessIntegrationTest.connector.newProcess(service, user, localhostName);
    }

    @Test
    public void connect() throws ProcessNotFoundException, IOException, InterruptedException, ServiceNotFoundException {
        final Process process1 = ProcessIntegrationTest.connector.getProcess(ProcessIntegrationTest.processId1);
        final Process process2 = ProcessIntegrationTest.connector.getProcess(ProcessIntegrationTest.processId2);

        final Connection connection = new Connection(process1.getId(),
                process2.getId(),
                ProcessIntegrationTest.service.getInterfaces().iterator().next().getName(),
                ProcessIntegrationTest.service.getInterfaces().iterator().next().getName());

        this.manager.addConnection(connection);

        Thread.sleep(100);
    }

    @AfterClass
    public static void stopProcess() throws ProcessNotFoundException {
        if (ProcessIntegrationTest.processId1 != null) {
            ProcessIntegrationTest.connector.removeProcess(ProcessIntegrationTest.processId1);
        }
        if (ProcessIntegrationTest.processId2 != null) {
            ProcessIntegrationTest.connector.removeProcess(ProcessIntegrationTest.processId2);
        }
    }

}
