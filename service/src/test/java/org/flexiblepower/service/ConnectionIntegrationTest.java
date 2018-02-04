/**
 * File ConnectionIntegrationTest.java
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
package org.flexiblepower.service;

import java.util.Arrays;
import java.util.List;

import org.flexiblepower.service.TestHandler.TestHandlerBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

// These tests are useful, but take very long
@RunWith(Parameterized.class)
@SuppressWarnings("static-method")
public class ConnectionIntegrationTest {

    /**
     *
     */
    private static final int WAIT_AFTER_CONNECT = 100;
    protected static int counter = 1;

    @Parameters
    public static List<Object[]> data() {
        return Arrays.asList(new Object[3][0]);
    }

    @After
    public void reset() throws InterruptedException {
        TestHandler.handlerMap.clear();
        ConnectionIntegrationTest.counter = 1;
        ServiceExecutor.getInstance().shutDown();
        System.gc();
        Thread.sleep(ConnectionIntegrationTest.WAIT_AFTER_CONNECT);
        System.out.println("Alles weer schoon!");
    }

    @Test(timeout = 10000)
    public void testNormalConnection() throws Exception {

        final InterfaceInfo info = TestHandler.class.getAnnotation(InterfaceInfo.class);
        ConnectionManager.registerConnectionHandlerFactory(TestHandler.class, new TestHandlerBuilder());

        try (
                final TCPConnection mc1 = new TCPConnection("CIT", 5000, "", info, "", "", "");
                final TCPConnection mc2 = new TCPConnection("CIT", 5000, "localhost", info, "", "", "")) {
            mc2.waitUntilConnected(0);
            Thread.sleep(ConnectionIntegrationTest.WAIT_AFTER_CONNECT); // Make sure at least 1 heartbeat is sent

            Assert.assertEquals("connected", TestHandler.handlerMap.get("h1").state);
            Assert.assertEquals("connected", TestHandler.handlerMap.get("h2").state);

            Assert.assertNull(TestHandler.handlerMap.get("h1").lastMessage);
            Assert.assertNotNull(TestHandler.handlerMap.get("h2").lastMessage);
            Assert.assertEquals("started", TestHandler.handlerMap.get("h2").lastMessage.getDebugInformation());

        }
        Thread.sleep(ConnectionIntegrationTest.WAIT_AFTER_CONNECT);
        Assert.assertEquals("terminated", TestHandler.handlerMap.get("h1").state);
        Assert.assertEquals("terminated", TestHandler.handlerMap.get("h2").state);
    }

    @Test(timeout = 15000)
    public void testSuspendAndResume() throws Exception {
        final InterfaceInfo info = TestHandler.class.getAnnotation(InterfaceInfo.class);
        ConnectionManager.registerConnectionHandlerFactory(TestHandler.class, new TestHandlerBuilder());

        try (
                final TCPConnection mc1 = new TCPConnection("CIT", 5000, "", info, "", "", "");
                final TCPConnection mc2 = new TCPConnection("CIT", 5000, "localhost", info, "", "", "")) {

            mc1.waitUntilConnected(0);
            Thread.sleep(ConnectionIntegrationTest.WAIT_AFTER_CONNECT);

            Assert.assertNotNull(TestHandler.handlerMap.get("h1"));
            Assert.assertNotNull(TestHandler.handlerMap.get("h2"));

            Assert.assertEquals("connected", TestHandler.handlerMap.get("h1").state);
            Assert.assertEquals("connected", TestHandler.handlerMap.get("h2").state);

            Assert.assertNull(TestHandler.handlerMap.get("h1").lastMessage);
            Assert.assertNotNull(TestHandler.handlerMap.get("h2").lastMessage);
            Assert.assertEquals("started", TestHandler.handlerMap.get("h2").lastMessage.getDebugInformation());

            mc1.goToSuspendedState();
            mc2.goToSuspendedState();

            Thread.sleep(ConnectionIntegrationTest.WAIT_AFTER_CONNECT);

            Assert.assertEquals("suspended", TestHandler.handlerMap.get("h1").state);
            Assert.assertEquals("suspended", TestHandler.handlerMap.get("h2").state);

            // Now we move mc2 to port 5002
            mc1.goToResumedState(5002, "");
            mc2.goToResumedState(5002, "localhost");

            mc1.waitUntilConnected(0);
            Thread.sleep(ConnectionIntegrationTest.WAIT_AFTER_CONNECT);

            Assert.assertNull(TestHandler.handlerMap.get("h1").lastMessage);
            Assert.assertEquals("resumed from suspend",
                    TestHandler.handlerMap.get("h2").lastMessage.getDebugInformation());

            Assert.assertEquals("resume-suspended", TestHandler.handlerMap.get("h1").state);
            Assert.assertEquals("resume-suspended", TestHandler.handlerMap.get("h2").state);
        }
    }

    @Test(timeout = 30000)
    public void testInterruptDetectionAndResume() throws Exception {
        final InterfaceInfo info = TestHandler.class.getAnnotation(InterfaceInfo.class);
        ConnectionManager.registerConnectionHandlerFactory(TestHandler.class, new TestHandler.TestHandlerBuilder());

        try (final TCPConnection mc2 = new TCPConnection("CIT", 5000, "localhost", info, "", "", "")) {
            try (final TCPConnection mc1 = new TCPConnection("CIT", 5000, "", info, "", "", "")) {
                mc1.waitUntilConnected(0);
                Thread.sleep(ConnectionIntegrationTest.WAIT_AFTER_CONNECT);

                Assert.assertNotNull(TestHandler.handlerMap.get("h1"));
                Assert.assertNotNull(TestHandler.handlerMap.get("h2"));
                Assert.assertEquals("connected", TestHandler.handlerMap.get("h1").state);
                Assert.assertEquals("connected", TestHandler.handlerMap.get("h2").state);

                Assert.assertNull(TestHandler.handlerMap.get("h1").lastMessage);
                Assert.assertNotNull(TestHandler.handlerMap.get("h2").lastMessage);
                Assert.assertEquals("started", TestHandler.handlerMap.get("h2").lastMessage.getDebugInformation());
            }

            Thread.sleep(ConnectionIntegrationTest.WAIT_AFTER_CONNECT);

            String state1 = TestHandler.handlerMap.get("h1").state;
            String state2 = TestHandler.handlerMap.get("h2").state;
            System.out.println("State 1 : " + state1);
            System.out.println("State 2 : " + state2);

            Assert.assertTrue(("terminated".equals(state1) && "interrupted".equals(state2))
                    || ("terminated".equals(state2) && "interrupted".equals(state1)));

            try (final TCPConnection mc3 = new TCPConnection("CIT", 5000, "", info, "", "", "")) {
                mc3.waitUntilConnected(0);
                Thread.sleep(ConnectionIntegrationTest.WAIT_AFTER_CONNECT);

                state1 = TestHandler.handlerMap.get("h1").state;
                state2 = TestHandler.handlerMap.get("h2").state;
                Assert.assertNotNull(TestHandler.handlerMap.get("h3"));
                final String state3 = TestHandler.handlerMap.get("h3").state;
                System.out.println("State 1 : " + state1);
                System.out.println("State 2 : " + state2);
                System.out.println("State 3 : " + state3);

                Assert.assertTrue(("connected".equals(state3) && "resume-interrupted".equals(state2))
                        || ("connected".equals(state3) && "resume-interrupted".equals(state1)));

                Assert.assertNull(TestHandler.handlerMap.get("h1").lastMessage);
                Assert.assertNotNull(TestHandler.handlerMap.get("h3").lastMessage);
                Assert.assertEquals("resumed from interrupt",
                        TestHandler.handlerMap.get("h3").lastMessage.getDebugInformation());
            }

            // Assert.assertEquals("connected", ConnectionIntegrationTest.handlerMap.get("h1").state);
            // Assert.assertEquals("resume-interrupted", ConnectionIntegrationTest.handlerMap.get("h2").state);
        }
    }
}
