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

import java.net.Socket;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bson.types.ObjectId;
import org.flexiblepower.connectors.MongoDbConnector;
import org.flexiblepower.connectors.RegistryConnector;
import org.flexiblepower.model.PrivateNode;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.Service;
import org.flexiblepower.model.UnidentifiedNode;
import org.flexiblepower.model.User;
import org.flexiblepower.process.ConnectionManager;
import org.flexiblepower.process.ProcessManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.swarm.Service.Criteria;

import lombok.extern.slf4j.Slf4j;

/**
 * ProcessIntegrationTest
 *
 * @version 0.1
 * @since Apr 24, 2017
 */
@Slf4j
@Disabled // This test is not supposed to run in CI
@SuppressWarnings("javadoc")
public class ProcessIntegrationTest {

    private static final String TEST_USER = "TestUser";
    private static final String TEST_PASS = "abc12345";

    final UserManager um = UserManager.getInstance();
    final NodeManager nm = NodeManager.getInstance();
    final ProcessManager pm = ProcessManager.getInstance();
    final ConnectionManager cm = ConnectionManager.getInstance();
    final ServiceManager sm = ServiceManager.getInstance();

    @BeforeAll
    public static void init() {
        String mongoHost = System.getenv(MongoDbConnector.MONGO_HOST_KEY);
        if (mongoHost == null) {
            mongoHost = "localhost";
        }
        String mongoPort = System.getenv(MongoDbConnector.MONGO_PORT_KEY);
        if (mongoPort == null) {
            mongoPort = "27017";
        }

        try (final Socket socket = new Socket(mongoHost, Integer.parseInt(mongoPort))) {
            // Do nothing
        } catch (final Exception e) {
            ProcessIntegrationTest.log.warn("Exception while connecting to MongoDb: {}", e.getMessage());
            Assumptions.assumeTrue(false, "Skipping tests because there is no Mongo service");
        }

        try {
            RegistryConnector.getInstance().getServices("services");
        } catch (final Exception e) {
            Assumptions.assumeTrue(false, "Skipping tests because there is no registry");
        }
    }

    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    public void runTest() throws Exception {
        // Get the user or create one
        User user = this.um.getUser(ProcessIntegrationTest.TEST_USER, ProcessIntegrationTest.TEST_PASS);
        if (user == null) {
            final ObjectId uid = this.um
                    .saveUser(new User(ProcessIntegrationTest.TEST_USER, ProcessIntegrationTest.TEST_PASS));
            user = this.um.getUser(uid);
        }
        Assertions.assertNotNull(user, "User not found");
        ProcessIntegrationTest.log.info("Found user {}", user);

        // Get a private node or create one
        List<PrivateNode> myNodes = this.nm.getPrivateNodesForUser(user);
        if (myNodes.isEmpty()) {
            final List<UnidentifiedNode> UNList = this.nm.getUnidentifiedNodes();
            Assumptions.assumeFalse(UNList.isEmpty());
            final UnidentifiedNode un = UNList.get(0);
            this.nm.makeUnidentifiedNodePrivate(un, user);
            myNodes = this.nm.getPrivateNodesForUser(user);
        }
        ProcessIntegrationTest.log.info("Using private node(s) {}", myNodes);

        // Get the two first private nodes
        final PrivateNode node1 = myNodes.get(0);
        final PrivateNode node2 = myNodes.get(1 % myNodes.size());
        Assertions.assertNotNull(node1, "Node 1 not found");
        Assertions.assertNotNull(node2, "Node 2 not found");

        // Get a service to instantiate
        final Service service = this.sm.listServices().get(0);
        Assertions.assertNotNull(service, "No service found");

        // Instantiate processes
        final Process process1 = this.pm.createProcess(
                Process.builder().serviceId(service.getId()).userId(user.getId()).privateNodeId(node1.getId()).build());
        final Process process2 = this.pm.createProcess(
                Process.builder().serviceId(service.getId()).userId(user.getId()).privateNodeId(node2.getId()).build());

        List<com.spotify.docker.client.messages.swarm.Service> servicesForP1;
        List<com.spotify.docker.client.messages.swarm.Service> servicesForP2;
        // Wait until we see that the processes are up and running

        try (final DockerClient client = DefaultDockerClient.fromEnv().build()) {
            do {
                Thread.sleep(Duration.ofSeconds(1).toMillis());
                servicesForP1 = client
                        .listServices(Criteria.builder().serviceName(process1.getId().toString()).build());
                ProcessIntegrationTest.log.info("Services for process 1: {}", servicesForP1);

                servicesForP2 = client
                        .listServices(Criteria.builder().serviceName(process2.getId().toString()).build());
                ProcessIntegrationTest.log.info("Services for process 2: {}", servicesForP2);
            } while (servicesForP1.isEmpty() || servicesForP2.isEmpty());

        }

        Assertions.assertFalse(servicesForP1.isEmpty());
        Assertions.assertFalse(servicesForP2.isEmpty());
    }

    @AfterEach
    public void cleanUp() throws InterruptedException {
        final User user = this.um.getUser(ProcessIntegrationTest.TEST_USER, ProcessIntegrationTest.TEST_PASS);

        try {
            // Try remove all processes
            final List<Process> myProcesses = this.pm.listProcessesForUser(user);
            for (final Process process : myProcesses) {
                try {
                    this.pm.deleteProcess(process);
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
