/*-
 * #%L
 * dEF-Pi API
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
