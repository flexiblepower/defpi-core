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

/**
 * MultipleFailureTest
 *
 * @author coenvl
 * @version 0.1
 * @since Nov 6, 2017
 */
@SuppressWarnings({"static-method", "javadoc"})
@RunWith(Parameterized.class)
public class MultipleFailureTest {

    /**
     *
     */
    private static final int TEST_PORT = 5001;

    @Parameters
    public static List<Object[]> data() {
        return Arrays.asList(new Object[3][0]);
    }

    @Rule
    public Timeout globalTimeout = Timeout.seconds(10);

    @Before
    public void before() throws InterruptedException {
        TestHandler.messageQueue.clear();
        TestHandler.stateQueue.clear();
        TestHandler.handlerMap.clear();
        ConnectionIntegrationTest.counter = 1;
    }

    @After
    public void after() {
        ServiceExecutor.getInstance().shutDown();
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
                Assert.assertEquals("connected", TestHandler.stateQueue.take());
                Assert.assertEquals("connected", TestHandler.stateQueue.take());
                Assert.assertEquals("started", TestHandler.messageQueue.take());
                Assert.assertEquals("started", TestHandler.messageQueue.take());

                for (int i = 0; i < 3; i++) {
                    if (Math.random() < 0.5) {
                        mc1.socket.close();
                    } else {
                        mc2.socket.close();
                    }

                    final String stateA = TestHandler.stateQueue.take();
                    final String stateB = TestHandler.stateQueue.take();

                    Assert.assertEquals("resumed from interrupt", TestHandler.messageQueue.take());
                    if (stateA.equals("interrupted")) {
                        Assert.assertEquals("resume-interrupted", TestHandler.stateQueue.take());
                    } else {
                        Assert.assertEquals("resume-interrupted", stateA);
                    }

                    Assert.assertEquals("resumed from interrupt", TestHandler.messageQueue.take());
                    if (stateB.equals("interrupted")) {
                        Assert.assertEquals("resume-interrupted", TestHandler.stateQueue.take());
                    } else {
                        Assert.assertEquals("resume-interrupted", stateB);
                    }

                    mc1.goToSuspendedState();
                    mc2.goToSuspendedState();
                    Assert.assertEquals("suspended", TestHandler.stateQueue.take());
                    Assert.assertEquals("suspended", TestHandler.stateQueue.take());

                    if (Math.random() < 0.5) {
                        mc1.goToResumedState(MultipleFailureTest.TEST_PORT, "");
                        mc2.goToResumedState(MultipleFailureTest.TEST_PORT, "localhost");
                    } else {
                        mc1.goToResumedState(MultipleFailureTest.TEST_PORT, "localhost");
                        mc2.goToResumedState(MultipleFailureTest.TEST_PORT, "");
                    }

                    Assert.assertEquals("resumed from suspend", TestHandler.messageQueue.take());
                    Assert.assertEquals("resumed from suspend", TestHandler.messageQueue.take());
                    Assert.assertEquals("resume-suspended", TestHandler.stateQueue.take());
                    Assert.assertEquals("resume-suspended", TestHandler.stateQueue.take());
                }
            }
            TestHandler.stateQueue.poll(100, TimeUnit.MILLISECONDS);
            TestHandler.stateQueue.poll(100, TimeUnit.MILLISECONDS);
        }
    }

}
