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

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import org.flexiblepower.commons.TCPSocket;
import org.flexiblepower.proto.ServiceProto.ErrorMessage;
import org.flexiblepower.proto.ServiceProto.GoToProcessStateMessage;
import org.flexiblepower.proto.ServiceProto.ProcessState;
import org.flexiblepower.proto.ServiceProto.ProcessStateUpdateMessage;
import org.flexiblepower.proto.ServiceProto.ResumeProcessMessage;
import org.flexiblepower.proto.ServiceProto.SetConfigMessage;
import org.flexiblepower.serializers.JavaIOSerializer;
import org.flexiblepower.serializers.MessageSerializer;
import org.flexiblepower.serializers.ProtobufMessageSerializer;
import org.flexiblepower.service.TestService.TestServiceConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;

import com.google.protobuf.ByteString;

/**
 * ServiceTest
 *
 * @version 0.1
 * @since May 12, 2017
 */
@SuppressWarnings("javadoc")
public class ServiceTest {

    private static final String TEST_HOST = "localhost";
    private static final String PROCESS_ID = "null";

    private final TestService testService = new TestService();
    private final MessageSerializer<Serializable> serializer = new JavaIOSerializer();
    private final ProtobufMessageSerializer pbSerializer = new ProtobufMessageSerializer();

    private ServiceManager<TestServiceConfiguration> manager;
    private TCPSocket managementSocket;

    @BeforeEach
    public void init() throws Exception {
        this.manager = new ServiceManager<>();
        try {
            this.manager.start(this.testService);
        } catch (final Exception e) {
            // In some JREs this will be a NPE, in others it will be a runtime exception
            if (!(e instanceof NullPointerException)) {
                Assertions.assertEquals(RuntimeException.class, e.getClass());
                Assertions.assertEquals(IllegalArgumentException.class, e.getCause().getClass());
                Assertions.assertTrue(e.getMessage().contains("protocol = http host = null"));
            }
        }

        this.managementSocket = TCPSocket.asClient(ServiceTest.TEST_HOST, ServiceManager.MANAGEMENT_PORT);

        this.pbSerializer.addMessageClass(GoToProcessStateMessage.class);
        this.pbSerializer.addMessageClass(SetConfigMessage.class);
        this.pbSerializer.addMessageClass(ProcessStateUpdateMessage.class);
        this.pbSerializer.addMessageClass(ResumeProcessMessage.class);
        this.pbSerializer.addMessageClass(ErrorMessage.class);

    }

    @RepeatedTest(3)
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void runReconnectTests() throws Exception {
        this.managementSocket.send("Rare string".getBytes());
        byte[] data = this.managementSocket.read();
        Object e = this.pbSerializer.deserialize(data);
        Assertions.assertEquals(ErrorMessage.class, e.getClass());
        Assertions.assertTrue(((ErrorMessage) e).getDebugInformation()
                .startsWith("org.flexiblepower.exceptions.SerializationException"));

        this.managementSocket.close();
        this.managementSocket = TCPSocket.asClient(ServiceTest.TEST_HOST, ServiceManager.MANAGEMENT_PORT);

        data = this.managementSocket.read(200);
        Assertions.assertNull(data);
        this.managementSocket.send("nog iets".getBytes());
        data = this.managementSocket.read();
        e = this.pbSerializer.deserialize(data);
        Assertions.assertEquals(ErrorMessage.class, e.getClass());
        Assertions.assertTrue(((ErrorMessage) e).getDebugInformation()
                .startsWith("org.flexiblepower.exceptions.SerializationException"));
    }

    @RepeatedTest(3)
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void runTests() throws Exception {
        // One test since they have to be executed in the correct order
        this.runConfigure();
        this.runReconfigure();
        this.runWithError();
        this.runSuspend();
    }

    @RepeatedTest(3)
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    public void runResumeTerminate() throws Exception {
        this.runResume();
        this.runConfigure();
        this.runTerminate();
    }

    public void runResume() throws Exception {
        final byte[] data = this.pbSerializer.serialize(ResumeProcessMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setStateData(ByteString.copyFrom(this.serializer.serialize(TestService.class)))
                .build());
        this.managementSocket.send(data);

        final byte[] received = this.managementSocket.read();
        Assertions.assertEquals(ProcessStateUpdateMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setState(ProcessState.RUNNING)
                .setStateData(ByteString.copyFrom("".getBytes()))
                .build(), this.pbSerializer.deserialize(received));
        Assertions.assertEquals("resumed", this.testService.stateQueue.take());
    }

    public void runConfigure() throws Exception {
        final byte[] msg = this.pbSerializer.serialize(SetConfigMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setIsUpdate(false)
                .putConfig("key", "value")
                .build());
        this.managementSocket.send(msg);
        final byte[] received = this.managementSocket.read();
        Assertions.assertArrayEquals(this.pbSerializer.serialize(ProcessStateUpdateMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setState(ProcessState.RUNNING)
                .setStateData(ByteString.EMPTY)
                .build()), received);
        Assertions.assertEquals("init", this.testService.stateQueue.take());
    }

    public void runWithError() throws Exception {
        final byte[] msg = this.pbSerializer.serialize(SetConfigMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setIsUpdate(false)
                .putConfig("makeMeThrowAnError", "true")
                .build());
        this.managementSocket.send(msg);
        final byte[] received = this.managementSocket.read();
        final Object err = this.pbSerializer.deserialize(received);
        Assertions.assertEquals(ErrorMessage.class, err.getClass());
        Assertions.assertEquals(ServiceTest.PROCESS_ID, ((ErrorMessage) err).getProcessId());
    }

    public void runReconfigure() throws Exception {
        this.managementSocket.send(this.pbSerializer.serialize(SetConfigMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setIsUpdate(true)
                .putConfig("key", "othervalue")
                .build()));
        Assertions.assertArrayEquals(this.pbSerializer.serialize(ProcessStateUpdateMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setState(ProcessState.RUNNING)
                .setStateData(ByteString.EMPTY)
                .build()), this.managementSocket.read());
        Assertions.assertEquals("modify", this.testService.stateQueue.take());
    }

    public void runSuspend() throws Exception {
        this.managementSocket.send(this.pbSerializer.serialize(GoToProcessStateMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setTargetState(ProcessState.SUSPENDED)
                .build()));
        final byte[] barr = this.managementSocket.read();
        Assertions.assertArrayEquals(this.pbSerializer.serialize(ProcessStateUpdateMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setState(ProcessState.SUSPENDED)
                .setStateData(ByteString.copyFrom(this.serializer.serialize(TestService.class)))
                .build()), barr);
        Assertions.assertEquals("suspend", this.testService.stateQueue.take());
    }

    public void runTerminate() throws Exception {
        this.managementSocket.send(this.pbSerializer.serialize(GoToProcessStateMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setTargetState(ProcessState.TERMINATED)
                .build()));
        Assertions.assertArrayEquals(this.pbSerializer.serialize(ProcessStateUpdateMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setState(ProcessState.TERMINATED)
                .setStateData(ByteString.EMPTY)
                .build()), this.managementSocket.read());
        Assertions.assertEquals("terminate", this.testService.stateQueue.take());
    }

    @AfterEach
    public void stop() throws InterruptedException {
        if (this.manager != null) {
            this.manager.close();
            this.manager = null;
        }
    }

}
