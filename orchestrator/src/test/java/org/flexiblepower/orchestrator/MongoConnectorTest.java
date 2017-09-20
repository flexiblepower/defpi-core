/**
 * File DockerConnectorTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator;

import org.flexiblepower.connectors.MongoDbConnector;
import org.junit.Test;

/**
 * DockerConnectorTest
 *
 * @author coenvl
 * @version 0.1
 * @since May 8, 2017
 */
public class MongoConnectorTest {

    @Test
    public void runTest() {
        System.out.println(MongoDbConnector.parseFilters("{\"stuff\":\"stuff\"}"));
    }

}
