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

import java.util.Map;

import org.flexiblepower.connectors.MongoDbConnector;
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

    @Test
    public void runTest() {
        final Map<String, Object> map = MongoDbConnector.parseFilters("{\"stuff\":\"stuff\"}");
        Assert.assertTrue(map.containsKey("stuff"));
        Assert.assertEquals("stuff", map.get("stuff"));
    }

}
