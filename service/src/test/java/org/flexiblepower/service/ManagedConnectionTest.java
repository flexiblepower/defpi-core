/**
 * File ManagedConnectionTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import org.flexiblepower.proto.ConnectionProto.ConnectionState;
import org.junit.Assert;
import org.junit.Test;

/**
 * ManagedConnectionTest
 *
 * @author coenvl
 * @version 0.1
 * @since May 29, 2017
 */
@SuppressWarnings("static-method")
public class ManagedConnectionTest {

    @Test(timeout = 5000)
    public void testConnection() throws Exception {
        try (
                TCPConnection conn = new TCPConnection("ConnID1234",
                        1234,
                        "tcp://localhost:5678",
                        TestService.class.getAnnotation(InterfaceInfo.class))) {
            Assert.assertEquals(ConnectionState.STARTING, conn.getState());
            conn.goToTerminatedState();
            Assert.assertEquals(ConnectionState.TERMINATED, conn.getState());
        }
    }

}
