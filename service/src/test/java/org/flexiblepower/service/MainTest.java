/*-
 * #%L
 * dEF-Pi service managing library
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

import org.flexiblepower.proto.ConnectionProto.ConnectionState;
import org.flexiblepower.serializers.ProtobufMessageSerializer;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * MainTest
 *
 * @author coenvl
 * @version 0.1
 * @since Nov 8, 2017
 */
@Ignore // Only run this test standalone in Eclipse, not as part of CI
@SuppressWarnings({"javadoc"})
public class MainTest {

    /**
     * MainConnectionHandler
     *
     * @author coenvl
     * @version 0.1
     * @since Nov 8, 2017
     */
    @InterfaceInfo(name = "MainHandler",
                   version = "V001",
                   receivesHash = "123",
                   sendsHash = "321",
                   sendTypes = {},
                   receiveTypes = {},
                   serializer = ProtobufMessageSerializer.class)
    public static class MainConnectionHandler implements ConnectionHandler {

        /**
         * @param c the Connection to handle
         */
        public MainConnectionHandler(final Connection c) {
            // Auto-generated constructor stub
        }

        @Override
        public void onSuspend() {
            // Auto-generated method stub

        }

        @Override
        public void resumeAfterSuspend() {
            // Auto-generated method stub

        }

        @Override
        public void onInterrupt() {
            // Auto-generated method stub

        }

        @Override
        public void resumeAfterInterrupt() {
            // Auto-generated method stub

        }

        @Override
        public void terminated() {
            // Auto-generated method stub

        }

    }

    @InterfaceInfo(name = "MainHandler",
                   version = "V002",
                   receivesHash = "1234",
                   sendsHash = "4321",
                   sendTypes = {},
                   receiveTypes = {},
                   serializer = ProtobufMessageSerializer.class)
    public static class MainConnectionHandler2 implements ConnectionHandler {

        /**
         * @param c the Connection to handle
         */
        public MainConnectionHandler2(final Connection c) {
            // Auto-generated constructor stub
        }

        @Override
        public void onSuspend() {
            // Auto-generated method stub

        }

        @Override
        public void resumeAfterSuspend() {
            // Auto-generated method stub

        }

        @Override
        public void onInterrupt() {
            // Auto-generated method stub

        }

        @Override
        public void resumeAfterInterrupt() {
            // Auto-generated method stub

        }

        @Override
        public void terminated() {
            // Auto-generated method stub

        }

    }

    private final Connection emptyConnection = new Connection() {

        @Override
        public void send(final Object message) {
            // Auto-generated method stub

        }

        @Override
        public boolean isConnected() {
            // Auto-generated method stub
            return false;
        }

        @Override
        public ConnectionState getState() {
            // Auto-generated method stub
            return null;
        }

        @Override
        public String remoteProcessId() {
            // Auto-generated method stub
            return null;
        }

        @Override
        public String remoteServiceId() {
            return null;
        }

        @Override
        public String remoteInterfaceId() {
            // Auto-generated method stub
            return null;
        }

    };

    @SuppressWarnings("static-method")
    public static class TestManager implements ConnectionHandlerManager {

        static int manager_creation_counter = 0;
        static int handler_creation_counter = 0;

        public TestManager() {
            TestManager.manager_creation_counter++;
        }

        public MainConnectionHandler buildV001(final Connection c) {
            TestManager.handler_creation_counter++;
            return new MainConnectionHandler(c);
        }

        public MainConnectionHandler2 buildV002(final Connection c) {
            TestManager.handler_creation_counter++;
            return new MainConnectionHandler2(c);
        }

    }

    @Test(timeout = 10000)
    public void runServiceMain() throws Exception {
        ServiceMain.main(new String[] {});
        ConnectionManager.buildHandlerForConnection(this.emptyConnection,
                MainConnectionHandler.class.getAnnotation(InterfaceInfo.class));
        ConnectionManager.buildHandlerForConnection(this.emptyConnection,
                MainConnectionHandler.class.getAnnotation(InterfaceInfo.class));
        ConnectionManager.buildHandlerForConnection(this.emptyConnection,
                MainConnectionHandler2.class.getAnnotation(InterfaceInfo.class));
        Assert.assertEquals(1, TestManager.manager_creation_counter);
        Assert.assertEquals(3, TestManager.handler_creation_counter);
    }

}
