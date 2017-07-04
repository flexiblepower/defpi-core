/**
 * File RegistryConnectorTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator;

import org.junit.Assert;
import org.junit.Test;

/**
 * RegistryConnectorTest
 *
 * @author coenvl
 * @version 0.1
 * @since Jul 4, 2017
 */
public class RegistryConnectorTest {

    @Test
    public void doRegistryConnectorTest() {
        final RegistryConnector conn = new RegistryConnector();
        Assert.assertNotNull(conn.listRepositories());
    }

}
