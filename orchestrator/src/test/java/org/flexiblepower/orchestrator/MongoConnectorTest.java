/**
 * File DockerConnectorTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator;

import java.util.Map;

import org.flexiblepower.connectors.MongoDbConnector;
import org.junit.Assert;
import org.junit.Test;

/**
 * DockerConnectorTest
 *
 * @author coenvl
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
