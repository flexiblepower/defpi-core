/**
 * File TestConnectionManager.java
 *
 * Copyright 2019 FAN
 */
package org.flexiblepower.service;

import org.flexiblepower.raml.client.TestClientConnectionHandler;
import org.flexiblepower.raml.server.TestServerConnectionHandler;

/**
 * TestConnectionManager
 *
 * @version 0.1
 * @since Sep 6, 2019
 */
@SuppressWarnings({"javadoc", "static-method"})
public class TestConnectionManager implements ConnectionHandlerManager {

    public static ConnectionManager cm = new ConnectionManager();

    static {
        final TestConnectionManager instant = new TestConnectionManager();
        ConnectionManager.registerConnectionHandlerFactory(TestServerConnectionHandler.class, instant);
        ConnectionManager.registerConnectionHandlerFactory(TestClientConnectionHandler.class, instant);
    }

    private TestConnectionManager() {
        // Private constructor
    }

    public TestServerConnectionHandler buildServer(final Connection c) {
        return new TestServerConnectionHandler(c);
    }

    public TestClientConnectionHandler buildClient(final Connection c) {
        return new TestClientConnectionHandler(c);
    }

    public static TestClientConnectionHandler getClientHandler(final Connection c) {
        return (TestClientConnectionHandler) ConnectionManager.buildHandlerForConnection(c,
                TestClientConnectionHandler.class.getAnnotation(InterfaceInfo.class));
    }

    public static TestServerConnectionHandler getServerHandler(final Connection c) {
        return (TestServerConnectionHandler) ConnectionManager.buildHandlerForConnection(c,
                TestServerConnectionHandler.class.getAnnotation(InterfaceInfo.class));
    }

}
