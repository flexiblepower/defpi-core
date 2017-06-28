/**
 * File ProcessIntegrationTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator;

import java.net.InetAddress;
import java.util.List;

import org.flexiblepower.model.Connection;
import org.flexiblepower.model.PrivateNode;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.Service;
import org.flexiblepower.model.UnidentifiedNode;
import org.flexiblepower.model.User;
import org.junit.Assert;
import org.junit.Test;

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

    private static final String TEST_USER = "Coen";
    private static final String TEST_PASS = "abc12345";
    private static final String TEST_SERVICE = "echo:0.0.1";

    @Test
    public void runTest() throws Exception {
        /*
         * try {
         * ProcessIntegrationTest.connector.newNetwork("user-net");
         * } catch (final Exception e) {
         * // Error creating network, probably already exists
         * }
         */

        final NodeManager nm = NodeManager.getInstance();
        final ProcessManager pm = ProcessManager.getInstance();
        final ConnectionManager cm = new ConnectionManager();
        final MongoDbConnector mdc = new MongoDbConnector();

        final Service service = ServiceManager.getInstance().getService("echo:0.0.1");
        User user = mdc.getUser(ProcessIntegrationTest.TEST_USER, ProcessIntegrationTest.TEST_PASS);

        if (user == null) {
            final String uid = mdc.createNewUser(ProcessIntegrationTest.TEST_USER, ProcessIntegrationTest.TEST_PASS);
            user = mdc.getUser(uid);
        }

        final List<UnidentifiedNode> nodeList = nm.getUnidentifiedNodes();
        for (final UnidentifiedNode node : nodeList) {
            nm.makeUnidentifiedNodePrivate(node, user);
        }

        final List<PrivateNode> myNodes = nm.getPrivateNodes();
        final PrivateNode node1 = myNodes.get(0);
        final PrivateNode node2 = myNodes.get(1 % myNodes.size());

        final Process process1 = pm.createProcess(
                Process.builder().serviceId(service.getId()).userId(user.getId()).privateNodeId(node1.getId()).build());

        final Process process2 = pm.createProcess(
                Process.builder().serviceId(service.getId()).userId(user.getId()).privateNodeId(node2.getId()).build());

        final String localhostName = InetAddress.getLocalHost().getHostName();

        Assert.assertTrue(cm.addConnection(new Connection(process1.getId(), process1.getId(), "Echo", "Echo")));
    }

    //
    // @Test
    // public void connect() throws ProcessNotFoundException, IOException, InterruptedException,
    // ServiceNotFoundException {
    // final Process process1 = ProcessIntegrationTest.connector.getProcess(ProcessIntegrationTest.processId1);
    // final Process process2 = ProcessIntegrationTest.connector.getProcess(ProcessIntegrationTest.processId2);
    //
    // final Connection connection = new Connection(process1.getDockerId(),
    // process2.getDockerId(),
    // ProcessIntegrationTest.service.getInterfaces().iterator().next().getName(),
    // ProcessIntegrationTest.service.getInterfaces().iterator().next().getName());
    //
    // this.manager.addConnection(connection);
    //
    // Thread.sleep(100);
    // }
    //
    // @AfterClass
    // public static void stopProcess() throws ProcessNotFoundException {
    // if (ProcessIntegrationTest.processId1 != null) {
    // ProcessIntegrationTest.connector.removeProcess(ProcessIntegrationTest.processId1);
    // }
    // if (ProcessIntegrationTest.processId2 != null) {
    // ProcessIntegrationTest.connector.removeProcess(ProcessIntegrationTest.processId2);
    // }
    // }

}
