/**
 * File ProcessIntegrationTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator;

import java.time.Duration;
import java.util.List;

import org.bson.types.ObjectId;
import org.flexiblepower.model.PrivateNode;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.Service;
import org.flexiblepower.model.UnidentifiedNode;
import org.flexiblepower.model.User;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.swarm.Service.Criteria;

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

    private static final String TEST_USER = "TestUser";
    private static final String TEST_PASS = "abc12345";

    final UserManager um = UserManager.getInstance();
    final NodeManager nm = NodeManager.getInstance();
    final ProcessManager pm = ProcessManager.getInstance();
    final ConnectionManager cm = ConnectionManager.getInstance();
    final ServiceManager sm = ServiceManager.getInstance();

    @Test(timeout = 80000)
    public void runTest() throws Exception {
        // Get the user or create one
        User user = this.um.getUser(ProcessIntegrationTest.TEST_USER, ProcessIntegrationTest.TEST_PASS);
        if (user == null) {
            final ObjectId uid = this.um.createNewUser(ProcessIntegrationTest.TEST_USER,
                    ProcessIntegrationTest.TEST_PASS);
            user = this.um.getUser(uid);
        }
        Assert.assertNotNull("User not found", user);
        ProcessIntegrationTest.log.info("Found user {}", user);

        // Get a private node or create one
        List<PrivateNode> myNodes = this.nm.getPrivateNodesForUser(user);
        if (myNodes.isEmpty()) {
            final UnidentifiedNode un = this.nm.getUnidentifiedNodes().get(0);
            this.nm.makeUnidentifiedNodePrivate(un, user);
            myNodes = this.nm.getPrivateNodesForUser(user);
        }
        ProcessIntegrationTest.log.info("Using private node(s) {}", myNodes);

        // Get the two first private nodes
        final PrivateNode node1 = myNodes.get(0);
        final PrivateNode node2 = myNodes.get(1 % myNodes.size());
        Assert.assertNotNull("Node 1 not found", node1);
        Assert.assertNotNull("Node 2 not found", node2);

        // Get a service to instantiate
        final Service service = this.sm.listServices().get(0);
        Assert.assertNotNull("No service found", service);

        // Instantiate processes
        final Process process1 = this.pm.createProcess(
                Process.builder().serviceId(service.getId()).userId(user.getId()).privateNodeId(node1.getId()).build());
        final Process process2 = this.pm.createProcess(
                Process.builder().serviceId(service.getId()).userId(user.getId()).privateNodeId(node2.getId()).build());

        // Wait until we see that the processes are up and running
        final DockerClient client = DefaultDockerClient.fromEnv().build();

        List<com.spotify.docker.client.messages.swarm.Service> servicesForP1;
        List<com.spotify.docker.client.messages.swarm.Service> servicesForP2;

        do {
            Thread.sleep(Duration.ofSeconds(1).toMillis());
            servicesForP1 = client.listServices(Criteria.builder().serviceName(process1.getId().toString()).build());
            ProcessIntegrationTest.log.info("Services for process 1: {}", servicesForP1);

            servicesForP2 = client.listServices(Criteria.builder().serviceName(process2.getId().toString()).build());
            ProcessIntegrationTest.log.info("Services for process 2: {}", servicesForP2);
        } while (servicesForP1.isEmpty() || servicesForP2.isEmpty());

        client.close();

        Assert.assertFalse(servicesForP1.isEmpty());
        Assert.assertFalse(servicesForP1.isEmpty());

        // Creating a connection can only be done when started as a container. From JUnit test, we cannot access the
        // user-net

    }

    @After
    public void cleanUp() throws InterruptedException {
        final User user = this.um.getUser(ProcessIntegrationTest.TEST_USER, ProcessIntegrationTest.TEST_PASS);

        try {
            // Try remove all processes
            final List<Process> myProcesses = this.pm.listProcesses(user);
            for (final Process process : myProcesses) {
                try {
                    this.pm.deleteProcess(process, true);
                    ProcessIntegrationTest.log.info("Removed process {}", process);
                } catch (final Throwable e) {
                    ProcessIntegrationTest.log.info("Unable to remove process {}", process);
                }
            }
        } catch (final Throwable e) {
            ProcessIntegrationTest.log.info("Error cleaning up processes: {}", e.getMessage());
        }

        try {
            // Try remove all nodes
            final List<PrivateNode> myNodes = this.nm.getPrivateNodesForUser(user);
            for (final PrivateNode pn : myNodes) {
                try {
                    this.nm.deletePrivateNode(pn);
                    ProcessIntegrationTest.log.info("Removed node {}", pn);
                } catch (final Throwable e) {
                    ProcessIntegrationTest.log.info("Unable to remove node {}", pn);
                }
            }
        } catch (final Throwable e) {
            ProcessIntegrationTest.log.info("Error cleaning up nodes: {}", e.getMessage());
        }

        try {
            // Remove user
            this.um.deleteUser(user);
        } catch (final Throwable e) {
            ProcessIntegrationTest.log.info("Error cleaning up user: {}", e.getMessage());
        }

    }

}