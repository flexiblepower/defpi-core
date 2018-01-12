/**
 * File MongoConnectorTest.java
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;

import org.bson.types.ObjectId;
import org.flexiblepower.connectors.MongoDbConnector;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.User;
import org.flexiblepower.process.ChangeProcessConfiguration;
import org.flexiblepower.process.CreateProcess.CreateDockerService;
import org.flexiblepower.process.TerminateProcess.RemoveDockerService;
import org.flexiblepower.process.TerminateProcess.SendTerminateSignal;
import org.junit.Assert;
import org.junit.Test;

/**
 * DockerConnectorTest
 *
 * @version 0.1
 * @since May 8, 2017
 */
@SuppressWarnings("static-method")
public class MongoConnectorTest {

    private static final String TEST_USER = "TEST_USER";
    private static final String TEST_PASS = "123456";
    private static final String TEST_SERVICE = "Tester";

    @Test
    public void runTest() {
        final Map<String, Object> map = MongoDbConnector.parseFilters("{\"stuff\":\"stuff\"}");
        Assert.assertTrue(map.containsKey("stuff"));
        Assert.assertEquals("stuff", map.get("stuff"));
    }

    @Test
    public void getNextPendingChange() {
        final MongoDbConnector mongo = MongoDbConnector.getInstance();
        System.out.println(mongo.getNextPendingChange());
    }

    // @Test
    public void nextPendingChangeTest() throws InterruptedException {
        final User testUser = UserManager.getInstance().getUser(MongoConnectorTest.TEST_USER);
        final ObjectId userId = testUser == null
                ? UserManager.getInstance().createNewUser(MongoConnectorTest.TEST_USER, MongoConnectorTest.TEST_PASS)
                : testUser.getId();
        final Process testProcess = Process.builder().userId(userId).serviceId(MongoConnectorTest.TEST_SERVICE).build();

        final MongoDbConnector mongo = MongoDbConnector.getInstance();
        mongo.save(new SendTerminateSignal(testProcess)); // Has as 2s delay
        mongo.save(new CreateDockerService(testProcess));
        mongo.save(new RemoveDockerService(testProcess)); // Has a 5s delay
        mongo.save(new ChangeProcessConfiguration(testProcess, new ArrayList<>()));

        Thread.sleep(Duration.ofSeconds(6).toMillis());

        Assert.assertEquals(CreateDockerService.class,
                MongoDbConnector.getInstance().getNextPendingChange().getClass());
        Assert.assertEquals(ChangeProcessConfiguration.class,
                MongoDbConnector.getInstance().getNextPendingChange().getClass());
        Assert.assertEquals(SendTerminateSignal.class,
                MongoDbConnector.getInstance().getNextPendingChange().getClass());
        Assert.assertEquals(RemoveDockerService.class,
                MongoDbConnector.getInstance().getNextPendingChange().getClass());

    }

}
