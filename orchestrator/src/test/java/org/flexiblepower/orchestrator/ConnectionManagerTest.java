/**
 * File ConnectionManagerTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator;

import java.util.UUID;

import org.flexiblepower.exceptions.ConnectionException;
import org.junit.Before;
import org.junit.Test;

/**
 * ConnectionManagerTest
 *
 * @author coenvl
 * @version 0.1
 * @since Apr 19, 2017
 */
public class ConnectionManagerTest {

    private ConnectionManager manager;
    private static final String TEST_HOST = "172.17.0.2";// "localhost";
    private static final int TEST_SERVICE_LISTEN_PORT = 5020;
    private static final int TEST_SERVICE_TARGET_PORT = 5025;
    private static final String ECHO_HASH = "eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252";

    private static final UUID TEST_CONNECTION_ID = UUID.randomUUID();

    @Before
    public void init() {
        this.manager = new ConnectionManager();
    }

    @Test
    public void tryConnect() throws ConnectionException {
        this.manager.connect(ConnectionManagerTest.TEST_CONNECTION_ID.toString(),
                ConnectionManagerTest.TEST_HOST,
                ConnectionManagerTest.TEST_SERVICE_LISTEN_PORT,
                ConnectionManagerTest.ECHO_HASH,
                "172.17.0.1",
                ConnectionManagerTest.TEST_SERVICE_TARGET_PORT,
                ConnectionManagerTest.ECHO_HASH);
    }

    @Test
    public void tryDisconnect() throws ConnectionException {
        this.manager.disconnect(ConnectionManagerTest.TEST_CONNECTION_ID.toString(), ConnectionManagerTest.TEST_HOST);
    }

}
