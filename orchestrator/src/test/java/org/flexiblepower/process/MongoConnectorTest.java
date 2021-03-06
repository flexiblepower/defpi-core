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
package org.flexiblepower.process;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.bson.types.ObjectId;
import org.flexiblepower.connectors.MongoDbConnector;
import org.flexiblepower.model.Connection;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.User;
import org.flexiblepower.orchestrator.UserManager;
import org.flexiblepower.orchestrator.pendingchange.PendingChange;
import org.flexiblepower.process.CreateProcess.CreateDockerService;
import org.flexiblepower.process.TerminateProcess.RemoveDockerService;
import org.flexiblepower.process.TerminateProcess.SendTerminateSignal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * DockerConnectorTest
 *
 * @version 0.1
 * @since May 8, 2017
 */
@SuppressWarnings({"static-method", "javadoc"})
@Disabled // Don't run this test, it will mess with existing pending changes
public class MongoConnectorTest {

    private static final String TEST_USER = "TEST_USER";
    private static final String TEST_PASS = "123456";
    private static final String TEST_SERVICE = "Tester";

    @BeforeAll
    public static void assumeMongo() {
        try {
            final MongoDbConnector mongo = MongoDbConnector.getInstance();
            while (mongo.getNextPendingChange() != null) {
                // Do nothing
            }
        } catch (final Exception e) {
            e.printStackTrace();
            Assumptions.assumeTrue(false);
        }
    }

    @Test
    public void runTest() {
        final Map<String, Object> map = MongoDbConnector.parseFilters("{\"stuff\":\"stuff\"}");
        Assertions.assertTrue(map.containsKey("stuff"));
        Assertions.assertEquals("stuff", map.get("stuff"));
    }

    @Test
    public void getNextPendingChange() {
        final MongoDbConnector mongo = MongoDbConnector.getInstance();
        System.out.println(mongo.getNextPendingChange());
    }

    @Test
    public void nextPendingChangeTest() throws InterruptedException {
        final User testUser = UserManager.getInstance().getUser(MongoConnectorTest.TEST_USER);
        final ObjectId userId = testUser == null
                ? UserManager.getInstance()
                        .saveUser(new User(MongoConnectorTest.TEST_USER, MongoConnectorTest.TEST_PASS))
                : testUser.getId();
        final Process testProcess = Process.builder().userId(userId).serviceId(MongoConnectorTest.TEST_SERVICE).build();

        final MongoDbConnector mongo = MongoDbConnector.getInstance();
        mongo.save(new SendTerminateSignal(testProcess)); // Has as 2s delay
        mongo.save(new CreateDockerService(testProcess));
        mongo.save(new RemoveDockerService(testProcess)); // Has a 5s delay
        mongo.save(new ChangeProcessConfiguration(testProcess, new ArrayList<>()));

        Thread.sleep(Duration.ofSeconds(6).toMillis());

        Assertions.assertEquals(CreateDockerService.class,
                MongoDbConnector.getInstance().getNextPendingChange().getClass());
        Assertions.assertEquals(ChangeProcessConfiguration.class,
                MongoDbConnector.getInstance().getNextPendingChange().getClass());
        Assertions.assertEquals(SendTerminateSignal.class,
                MongoDbConnector.getInstance().getNextPendingChange().getClass());
        Assertions.assertEquals(RemoveDockerService.class,
                MongoDbConnector.getInstance().getNextPendingChange().getClass());
    }

    @Test
    public void lockedPendingChangeTest() throws InterruptedException {
        final User testUser = UserManager.getInstance().getUser(MongoConnectorTest.TEST_USER);

        final ObjectId userId = testUser == null
                ? UserManager.getInstance()
                        .saveUser(new User(MongoConnectorTest.TEST_USER, MongoConnectorTest.TEST_PASS))
                : testUser.getId();

        final Process testProcess1 = Process.builder()
                .id(new ObjectId())
                .userId(userId)
                .serviceId(MongoConnectorTest.TEST_SERVICE)
                .build();

        final Process testProcess2 = Process.builder()
                .id(new ObjectId())
                .userId(userId)
                .serviceId(MongoConnectorTest.TEST_SERVICE)
                .build();

        final Connection testConnection = new Connection(new ObjectId(),
                new Connection.Endpoint(testProcess1.getId(), "Something"),
                new Connection.Endpoint(testProcess2.getId(), "Something"));

        final MongoDbConnector mongo = MongoDbConnector.getInstance();
        mongo.save(new SendTerminateSignal(testProcess1)); // Has as 2s delay
        mongo.save(new CreateDockerService(testProcess2));
        mongo.save(new RemoveDockerService(testProcess2)); // Has a 5s delay
        mongo.save(new ChangeProcessConfiguration(testProcess1, new ArrayList<>()));
        mongo.save(new CreateConnectionEndpoint(userId, testConnection, testConnection.getEndpoint1()));
        mongo.save(new CreateConnectionEndpoint(userId, testConnection, testConnection.getEndpoint2()));

        Thread.sleep(Duration.ofSeconds(6).toMillis());

        Assertions.assertEquals(ChangeProcessConfiguration.class,
                MongoDbConnector.getInstance()
                        .getNextPendingChange(Arrays.asList(testConnection.getId(), testProcess2.getId()))
                        .getClass());

        Assertions.assertEquals(SendTerminateSignal.class,
                MongoDbConnector.getInstance()
                        .getNextPendingChange(Arrays.asList(testConnection.getId(), testProcess2.getId()))
                        .getClass());

        Assertions.assertNull(MongoDbConnector.getInstance()
                .getNextPendingChange(Arrays.asList(testConnection.getId(), testProcess2.getId())));

        PendingChange pc = MongoDbConnector.getInstance().getNextPendingChange(Arrays.asList(testProcess2.getId()));
        Assertions.assertEquals(CreateConnectionEndpoint.class, pc.getClass());
        Assertions.assertEquals(
                "Creating connection for Process " + testProcess1.getId() + " with interface Something to process "
                        + testProcess2.getId() + " with connection Something",
                ((CreateConnectionEndpoint) pc).description());

        Assertions.assertEquals(CreateDockerService.class,
                MongoDbConnector.getInstance().getNextPendingChange().getClass());
        pc = MongoDbConnector.getInstance().getNextPendingChange();
        Assertions.assertEquals(CreateConnectionEndpoint.class, pc.getClass());
        Assertions.assertEquals(
                "Creating connection for Process " + testProcess2.getId() + " with interface Something to process "
                        + testProcess1.getId() + " with connection Something",
                ((CreateConnectionEndpoint) pc).description());
        Assertions.assertEquals(RemoveDockerService.class,
                MongoDbConnector.getInstance().getNextPendingChange().getClass());

    }

    @AfterEach
    public void cleanup() {
        try {
            final MongoDbConnector mongo = MongoDbConnector.getInstance();
            while (mongo.getNextPendingChange() != null) {
                // Do nothing
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

}
