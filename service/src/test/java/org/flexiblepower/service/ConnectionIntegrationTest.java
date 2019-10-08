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

import java.util.concurrent.TimeUnit;

import org.flexiblepower.service.TestHandler.TestHandlerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;

@Timeout(value = 5, unit = TimeUnit.SECONDS)
@SuppressWarnings({"static-method", "javadoc"})
public class ConnectionIntegrationTest {

    private static final int TEST_PORT = 5001;
    protected static int counter = 1;

    @BeforeEach
    public void before() throws InterruptedException {
        TestHandler.handlerMap.clear();
        TestHandler.messageQueue.clear();
        TestHandler.stateQueue.clear();
        ConnectionIntegrationTest.counter = 1;
    }

    @AfterEach
    public void after() {
        ServiceExecutor.getInstance().shutDown();
    }

    @RepeatedTest(3)
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
            Assertions.assertEquals("connected", TestHandler.stateQueue.take());
            Assertions.assertEquals("connected", TestHandler.stateQueue.take());
            Assertions.assertEquals("started", TestHandler.messageQueue.take());
            Assertions.assertEquals("started", TestHandler.messageQueue.take());
        }
        TestHandler.stateQueue.poll(100, TimeUnit.MILLISECONDS);
        TestHandler.stateQueue.poll(100, TimeUnit.MILLISECONDS);
    }

    @RepeatedTest(3)
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

            Assertions.assertEquals("connected", TestHandler.stateQueue.take());
            Assertions.assertEquals("connected", TestHandler.stateQueue.take());
            Assertions.assertEquals("started", TestHandler.messageQueue.take());
            Assertions.assertEquals("started", TestHandler.messageQueue.take());

            mc1.goToSuspendedState();
            mc2.goToSuspendedState();
            Assertions.assertEquals("suspended", TestHandler.stateQueue.take());
            Assertions.assertEquals("suspended", TestHandler.stateQueue.take());

            // Now we move mc2 to port 5002
            mc1.goToResumedState(5002, "");
            mc2.goToResumedState(5002, "localhost");

            Assertions.assertEquals("resumed from suspend", TestHandler.messageQueue.take());
            Assertions.assertEquals("resumed from suspend", TestHandler.messageQueue.take());
            Assertions.assertEquals("resume-suspended", TestHandler.stateQueue.take());
            Assertions.assertEquals("resume-suspended", TestHandler.stateQueue.take());

        }
        TestHandler.stateQueue.poll(100, TimeUnit.MILLISECONDS);
        TestHandler.stateQueue.poll(100, TimeUnit.MILLISECONDS);
    }

    @RepeatedTest(3)
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
                Assertions.assertEquals("connected", TestHandler.stateQueue.take());
                Assertions.assertEquals("connected", TestHandler.stateQueue.take());
                Assertions.assertEquals("started", TestHandler.messageQueue.take());
                Assertions.assertEquals("started", TestHandler.messageQueue.take());

                Assertions.assertNotNull(TestHandler.handlerMap.get("h1"));
                Assertions.assertNotNull(TestHandler.handlerMap.get("h2"));
            }

            String state1 = TestHandler.stateQueue.take();
            String state2 = TestHandler.stateQueue.take();

            Assertions.assertTrue(("terminated".equals(state1) && "interrupted".equals(state2))
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
                Assertions.assertTrue(("started".equals(recv1) && "resumed from interrupt".equals(recv2))
                        || ("started".equals(recv2) && "resumed from interrupt".equals(recv1)));
                Assertions.assertNotNull(TestHandler.handlerMap.get("h3"));

                state1 = TestHandler.stateQueue.take();
                state2 = TestHandler.stateQueue.take();
                System.out.println(state1 + ", " + state2);
                Assertions.assertTrue(("connected".equals(state2) && "resume-interrupted".equals(state1))
                        || ("connected".equals(state1) && "resume-interrupted".equals(state2)));

                Assertions.assertEquals("resumed from interrupt",
                        TestHandler.handlerMap.get("h3").lastMessage.getDebugInformation());
            }
        }
        TestHandler.stateQueue.poll(100, TimeUnit.MILLISECONDS);
        TestHandler.stateQueue.poll(100, TimeUnit.MILLISECONDS);
    }
}
