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

/**
 * MultipleFailureTest
 *
 * @author coenvl
 * @version 0.1
 * @since Nov 6, 2017
 */
@SuppressWarnings({"static-method", "javadoc"})
@Timeout(value = 10, unit = TimeUnit.SECONDS)
public class MultipleFailureTest {

    /**
     *
     */
    private static final int TEST_PORT = 5001;

    @BeforeEach
    public void before() throws InterruptedException {
        TestHandler.messageQueue.clear();
        TestHandler.stateQueue.clear();
        TestHandler.handlerMap.clear();
        ConnectionIntegrationTest.counter = 1;
    }

    @AfterEach
    public void after() {
        ServiceExecutor.getInstance().shutDown();
    }

    @RepeatedTest(3)
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
                Assertions.assertEquals("connected", TestHandler.stateQueue.take());
                Assertions.assertEquals("connected", TestHandler.stateQueue.take());
                Assertions.assertEquals("started", TestHandler.messageQueue.take());
                Assertions.assertEquals("started", TestHandler.messageQueue.take());

                for (int i = 0; i < 3; i++) {
                    if (Math.random() < 0.5) {
                        mc1.socket.close();
                    } else {
                        mc2.socket.close();
                    }

                    final String stateA = TestHandler.stateQueue.take();
                    final String stateB = TestHandler.stateQueue.take();

                    Assertions.assertEquals("resumed from interrupt", TestHandler.messageQueue.take());
                    if (stateA.equals("interrupted")) {
                        Assertions.assertEquals("resume-interrupted", TestHandler.stateQueue.take());
                    } else {
                        Assertions.assertEquals("resume-interrupted", stateA);
                    }

                    Assertions.assertEquals("resumed from interrupt", TestHandler.messageQueue.take());
                    if (stateB.equals("interrupted")) {
                        Assertions.assertEquals("resume-interrupted", TestHandler.stateQueue.take());
                    } else {
                        Assertions.assertEquals("resume-interrupted", stateB);
                    }

                    mc1.goToSuspendedState();
                    mc2.goToSuspendedState();
                    Assertions.assertEquals("suspended", TestHandler.stateQueue.take());
                    Assertions.assertEquals("suspended", TestHandler.stateQueue.take());

                    if (Math.random() < 0.5) {
                        mc1.goToResumedState(MultipleFailureTest.TEST_PORT, "");
                        mc2.goToResumedState(MultipleFailureTest.TEST_PORT, "localhost");
                    } else {
                        mc1.goToResumedState(MultipleFailureTest.TEST_PORT, "localhost");
                        mc2.goToResumedState(MultipleFailureTest.TEST_PORT, "");
                    }

                    Assertions.assertEquals("resumed from suspend", TestHandler.messageQueue.take());
                    Assertions.assertEquals("resumed from suspend", TestHandler.messageQueue.take());
                    Assertions.assertEquals("resume-suspended", TestHandler.stateQueue.take());
                    Assertions.assertEquals("resume-suspended", TestHandler.stateQueue.take());
                }
            }
            TestHandler.stateQueue.poll(100, TimeUnit.MILLISECONDS);
            TestHandler.stateQueue.poll(100, TimeUnit.MILLISECONDS);
        }
    }

}
