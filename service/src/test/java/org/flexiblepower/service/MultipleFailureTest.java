/**
 * File MultipleFailureTest.java
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runners.Parameterized.Parameters;

/**
 * MultipleFailureTest
 *
 * @author coenvl
 * @version 0.1
 * @since Nov 6, 2017
 */
@SuppressWarnings({"static-method", "javadoc"})
public class MultipleFailureTest {

    /**
     *
     */
    private static final int TEST_PORT = 5001;
    private static final int WAIT_AFTER_CONNECT = 200;

    @Parameters
    public static List<Object[]> data() {
        return Arrays.asList(new Object[3][0]);
    }

    @Rule
    public Timeout globalTimeout = Timeout.seconds(10);

    @After
    public void reset() throws InterruptedException {
        TestHandler.handlerMap.clear();
        ConnectionIntegrationTest.counter = 1;
        ServiceExecutor.getInstance().shutDown();
        Thread.sleep(MultipleFailureTest.WAIT_AFTER_CONNECT);
    }

    @Test
    public void testMultipleInterruptDetectionAndResume() throws Exception {
        final InterfaceInfo info = TestHandler.class.getAnnotation(InterfaceInfo.class);
        ConnectionManager.registerConnectionHandlerFactory(TestHandler.class, new TestHandlerBuilder());

        try (
                final TCPConnection mc2 = new TCPConnection("CIT",
                        MultipleFailureTest.TEST_PORT,
                        "localhost",
                        info,
                        "",
                        "",
                        "")) {
            try (
                    final TCPConnection mc1 = new TCPConnection("CIT",
                            MultipleFailureTest.TEST_PORT,
                            "",
                            info,
                            "",
                            "",
                            "")) {
                mc1.waitUntilConnected();
                Thread.sleep(MultipleFailureTest.WAIT_AFTER_CONNECT);

                Assert.assertNotNull(TestHandler.handlerMap.get("h1"));
                Assert.assertNotNull(TestHandler.handlerMap.get("h2"));
                Assert.assertEquals("connected", TestHandler.handlerMap.get("h1").state);
                Assert.assertEquals("connected", TestHandler.handlerMap.get("h2").state);

                Assert.assertNull(TestHandler.handlerMap.get("h1").lastMessage);
                Assert.assertNotNull(TestHandler.handlerMap.get("h2").lastMessage);
                Assert.assertEquals("started", TestHandler.handlerMap.get("h2").lastMessage.getDebugInformation());

                for (int i = 0; i < 3; i++) {
                    if (Math.random() < 0.5) {
                        mc1.socket.close();
                    } else {
                        mc2.socket.close();
                    }

                    // Should fix itself...
                    Thread.sleep(MultipleFailureTest.WAIT_AFTER_CONNECT);
                    mc1.waitUntilConnected();
                    mc2.waitUntilConnected();
                    Assert.assertEquals("resume-interrupted", TestHandler.handlerMap.get("h1").state);
                    Assert.assertEquals("resume-interrupted", TestHandler.handlerMap.get("h2").state);

                    mc1.goToSuspendedState();
                    mc2.goToSuspendedState();
                    Thread.sleep(MultipleFailureTest.WAIT_AFTER_CONNECT);
                    Assert.assertEquals("suspended", TestHandler.handlerMap.get("h1").state);
                    Assert.assertEquals("suspended", TestHandler.handlerMap.get("h2").state);

                    if (Math.random() < 0.5) {
                        mc1.goToResumedState(MultipleFailureTest.TEST_PORT, "");
                        mc2.goToResumedState(MultipleFailureTest.TEST_PORT, "localhost");
                    } else {
                        mc1.goToResumedState(MultipleFailureTest.TEST_PORT, "localhost");
                        mc2.goToResumedState(MultipleFailureTest.TEST_PORT, "");
                    }
                    Thread.sleep(MultipleFailureTest.WAIT_AFTER_CONNECT);
                    mc1.waitUntilConnected();
                    mc2.waitUntilConnected();

                    Assert.assertEquals("resume-suspended", TestHandler.handlerMap.get("h1").state);
                    Assert.assertEquals("resume-suspended", TestHandler.handlerMap.get("h2").state);
                }
            }

        }
    }

}
