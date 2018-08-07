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
import java.util.concurrent.TimeUnit;

import org.flexiblepower.service.TestHandler.TestHandlerBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
@SuppressWarnings({"static-method", "javadoc"})
public class ConnectionIntegrationTest {

    private static final int TEST_PORT = 5001;
    protected static int counter = 1;

    @Parameters
    public static List<Object[]> data() {
        return Arrays.asList(new Object[300][0]);
    }

    @Rule
    public Timeout globalTimeout = Timeout.seconds(5);

    @Before
    public void before() throws InterruptedException {
        TestHandler.handlerMap.clear();
        TestHandler.messageQueue.clear();
        TestHandler.stateQueue.clear();
        ConnectionIntegrationTest.counter = 1;
    }

    @After
    public void after() {
        ServiceExecutor.getInstance().shutDown();
    }

    @Test
    public void testNormalConnection() throws Exception {

        final InterfaceInfo info = TestHandler.class.getAnnotation(InterfaceInfo.class);
        ConnectionManager.registerConnectionHandlerFactory(TestHandler.class, new TestHandlerBuilder());

        try (
                final TCPConnection mc1 = new TCPConnection("CIT",
                        ConnectionIntegrationTest.TEST_PORT,
                        "",
                        info,
                        "",
                        "",
                        "");
                final TCPConnection mc2 = new TCPConnection("CIT",
                        ConnectionIntegrationTest.TEST_PORT,
                        "localhost",
                        info,
                        "",
                        "",
                        "")) {
            Assert.assertEquals("connected", TestHandler.stateQueue.take());
            Assert.assertEquals("connected", TestHandler.stateQueue.take());
            Assert.assertEquals("started", TestHandler.messageQueue.take());
            Assert.assertEquals("started", TestHandler.messageQueue.take());
        }
        TestHandler.stateQueue.poll(100, TimeUnit.MILLISECONDS);
        TestHandler.stateQueue.poll(100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testSuspendAndResume() throws Exception {
        final InterfaceInfo info = TestHandler.class.getAnnotation(InterfaceInfo.class);
        ConnectionManager.registerConnectionHandlerFactory(TestHandler.class, new TestHandlerBuilder());

        try (
                final TCPConnection mc1 = new TCPConnection("CIT",
                        ConnectionIntegrationTest.TEST_PORT,
                        "",
                        info,
                        "",
                        "",
                        "");
                final TCPConnection mc2 = new TCPConnection("CIT",
                        ConnectionIntegrationTest.TEST_PORT,
                        "localhost",
                        info,
                        "",
                        "",
                        "")) {

            Assert.assertEquals("connected", TestHandler.stateQueue.take());
            Assert.assertEquals("connected", TestHandler.stateQueue.take());
            Assert.assertEquals("started", TestHandler.messageQueue.take());
            Assert.assertEquals("started", TestHandler.messageQueue.take());

            mc1.goToSuspendedState();
            mc2.goToSuspendedState();
            Assert.assertEquals("suspended", TestHandler.stateQueue.take());
            Assert.assertEquals("suspended", TestHandler.stateQueue.take());

            // Now we move mc2 to port 5002
            mc1.goToResumedState(5002, "");
            mc2.goToResumedState(5002, "localhost");

            Assert.assertEquals("resumed from suspend", TestHandler.messageQueue.take());
            Assert.assertEquals("resumed from suspend", TestHandler.messageQueue.take());
            Assert.assertEquals("resume-suspended", TestHandler.stateQueue.take());
            Assert.assertEquals("resume-suspended", TestHandler.stateQueue.take());

        }
        TestHandler.stateQueue.poll(100, TimeUnit.MILLISECONDS);
        TestHandler.stateQueue.poll(100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testInterruptDetectionAndResume() throws Exception {
        final InterfaceInfo info = TestHandler.class.getAnnotation(InterfaceInfo.class);
        ConnectionManager.registerConnectionHandlerFactory(TestHandler.class, new TestHandler.TestHandlerBuilder());

        try (
                final TCPConnection mc2 = new TCPConnection("CIT",
                        ConnectionIntegrationTest.TEST_PORT,
                        "localhost",
                        info,
                        "",
                        "",
                        "")) {
            try (
                    final TCPConnection mc1 = new TCPConnection("CIT",
                            ConnectionIntegrationTest.TEST_PORT,
                            "",
                            info,
                            "",
                            "",
                            "")) {
                Assert.assertEquals("connected", TestHandler.stateQueue.take());
                Assert.assertEquals("connected", TestHandler.stateQueue.take());
                Assert.assertEquals("started", TestHandler.messageQueue.take());
                Assert.assertEquals("started", TestHandler.messageQueue.take());

                Assert.assertNotNull(TestHandler.handlerMap.get("h1"));
                Assert.assertNotNull(TestHandler.handlerMap.get("h2"));
            }

            String state1 = TestHandler.stateQueue.take();
            String state2 = TestHandler.stateQueue.take();

            Assert.assertTrue(("terminated".equals(state1) && "interrupted".equals(state2))
                    || ("terminated".equals(state2) && "interrupted".equals(state1)));

            try (
                    final TCPConnection mc3 = new TCPConnection("CIT",
                            ConnectionIntegrationTest.TEST_PORT,
                            "",
                            info,
                            "",
                            "",
                            "")) {
                final String recv1 = TestHandler.messageQueue.take();
                final String recv2 = TestHandler.messageQueue.take();
                Assert.assertTrue(("started".equals(recv1) && "resumed from interrupt".equals(recv2))
                        || ("started".equals(recv2) && "resumed from interrupt".equals(recv1)));
                Assert.assertNotNull(TestHandler.handlerMap.get("h3"));

                state1 = TestHandler.stateQueue.take();
                state2 = TestHandler.stateQueue.take();
                System.out.println(state1 + ", " + state2);
                Assert.assertTrue(("connected".equals(state2) && "resume-interrupted".equals(state1))
                        || ("connected".equals(state1) && "resume-interrupted".equals(state2)));

                Assert.assertEquals("resumed from interrupt",
                        TestHandler.handlerMap.get("h3").lastMessage.getDebugInformation());
            }
        }
        TestHandler.stateQueue.poll(100, TimeUnit.MILLISECONDS);
        TestHandler.stateQueue.poll(100, TimeUnit.MILLISECONDS);
    }
}
