/**
 * File ProcessIntegrationTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator;

import java.util.UUID;

import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.PrivateNode;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.Service;
import org.flexiblepower.proto.ServiceProto.ConnectionMessage;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;

/**
 * ProcessIntegrationTest
 *
 * @author coenvl
 * @version 0.1
 * @since Apr 24, 2017
 */
public class ProcessIntegrationTest {

    private static final RegistryConnector registry = new RegistryConnector();
    private static final DockerConnector connector = new DockerConnector();

    private static String processId;

    @BeforeClass
    public static void startClass()
            throws ServiceNotFoundException, DockerException, InterruptedException, DockerCertificateException {
        final Service service = ProcessIntegrationTest.registry.getService("services", "echo", "0.0.1");

        System.out.println(DockerConnector.init().listNodes());

        final Process process = new Process();
        process.setProcessService(service);
        process.setUserName("TestUser3");
        process.setRunningNode(new PrivateNode("def-pi1", process.getUserName()));

        System.out.println(ProcessIntegrationTest.connector.listProcesses());

        ProcessIntegrationTest.processId = ProcessIntegrationTest.connector.newProcess(process);
        System.out.println(ProcessIntegrationTest.processId);
    }

    @Test
    public void connect() {
        final ConnectionMessage connection = ConnectionMessage.newBuilder()
                .setConnectionId(UUID.randomUUID().toString())
                .setMode(ConnectionMessage.ModeType.CREATE)
                .setTargetAddress("tcp://localhost:5051")
                .setListenPort(5025)
                .setReceiveHash("eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252")
                .setSendHash("eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252")
                .build();

        Assert.assertEquals(0, ConnectionManager.sendStartSession("localhost", connection));
    }

    @AfterClass
    public static void stopProcess() throws ProcessNotFoundException {
        // ProcessIntegrationTest.connector.removeProcess(ProcessIntegrationTest.processId);
    }

}
