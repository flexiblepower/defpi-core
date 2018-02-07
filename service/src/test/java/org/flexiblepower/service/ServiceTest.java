/**
 * File ServiceTest.java
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

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

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
import org.flexiblepower.service.exceptions.ServiceInvocationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.protobuf.ByteString;

/**
 * ServiceTest
 *
 * @version 0.1
 * @since May 12, 2017
 */
@RunWith(Parameterized.class)
@SuppressWarnings("javadoc")
public class ServiceTest {

    @Parameters
    public static List<Object[]> data() {
        return Arrays.asList(new Object[3][0]);
    }

    private static final String TEST_HOST = "localhost";
    private static final String PROCESS_ID = "null";

    private final TestService testService = new TestService();
    private final MessageSerializer<Serializable> serializer = new JavaIOSerializer();
    private final ProtobufMessageSerializer pbSerializer = new ProtobufMessageSerializer();

    private ServiceManager<TestServiceConfiguration> manager;
    private TCPSocket managementSocket;

    @Before
    public void init() throws Exception {
        this.manager = new ServiceManager<>();
        try {
            this.manager.start(this.testService);
        } catch (final ServiceInvocationException e) {
            Assert.assertTrue(e.getMessage().startsWith("Futile"));
        }

        this.managementSocket = TCPSocket.asClient(ServiceTest.TEST_HOST, ServiceManager.MANAGEMENT_PORT);

        this.pbSerializer.addMessageClass(GoToProcessStateMessage.class);
        this.pbSerializer.addMessageClass(SetConfigMessage.class);
        this.pbSerializer.addMessageClass(ProcessStateUpdateMessage.class);
        this.pbSerializer.addMessageClass(ResumeProcessMessage.class);
        this.pbSerializer.addMessageClass(ErrorMessage.class);
    }

    @Test(timeout = 10000)
    public void runReconnectTests() throws Exception {
        this.managementSocket.send("Rare string".getBytes());
        byte[] data = this.managementSocket.read();
        Object e = this.pbSerializer.deserialize(data);
        Assert.assertEquals(ErrorMessage.class, e.getClass());
        Assert.assertTrue(((ErrorMessage) e).getDebugInformation()
                .startsWith("org.flexiblepower.exceptions.SerializationException"));

        Thread.sleep(200);
        this.managementSocket.close();

        Thread.sleep(100);

        this.managementSocket = TCPSocket.asClient(ServiceTest.TEST_HOST, ServiceManager.MANAGEMENT_PORT);

        data = this.managementSocket.read(200);
        Assert.assertNull(data);
        Thread.sleep(100);
        this.managementSocket.send("nog iets".getBytes());
        data = this.managementSocket.read();
        e = this.pbSerializer.deserialize(data);
        Assert.assertEquals(ErrorMessage.class, e.getClass());
        Assert.assertTrue(((ErrorMessage) e).getDebugInformation()
                .startsWith("org.flexiblepower.exceptions.SerializationException"));
    }

    @Test(timeout = 60000)
    public void runTests() throws Exception {
        // One test since they have to be executed in the correct order
        this.runConfigure();
        this.runReconfigure();
        this.runWithError();
        this.runSuspend();
    }

    @Test(timeout = 60000)
    public void runResumeTerminate() throws Exception {
        this.runResume();
        this.runTerminate();
    }

    public void runResume() throws Exception {
        final byte[] data = this.pbSerializer.serialize(ResumeProcessMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setStateData(ByteString.copyFrom(this.serializer.serialize(TestService.class)))
                .build());
        this.managementSocket.send(data);

        final byte[] received = this.managementSocket.read();
        Assert.assertEquals(
                ProcessStateUpdateMessage.newBuilder()
                        .setProcessId(ServiceTest.PROCESS_ID)
                        .setState(ProcessState.RUNNING)
                        .setStateData(ByteString.copyFrom("".getBytes()))
                        .build(),
                this.pbSerializer.deserialize(received));
        Thread.sleep(100);
        Assert.assertEquals("resumed", this.testService.getState());
    }

    public void runConfigure() throws Exception {
        final byte[] msg = this.pbSerializer.serialize(SetConfigMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setIsUpdate(false)
                .putConfig("key", "value")
                .build());
        this.managementSocket.send(msg);
        final byte[] received = this.managementSocket.read();
        Assert.assertArrayEquals(this.pbSerializer.serialize(ProcessStateUpdateMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setState(ProcessState.RUNNING)
                .setStateData(ByteString.EMPTY)
                .build()), received);
        Thread.sleep(100);
        Assert.assertEquals("init", this.testService.getState());
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
        Assert.assertEquals(ErrorMessage.class, err.getClass());
        Assert.assertEquals(ServiceTest.PROCESS_ID, ((ErrorMessage) err).getProcessId());
    }

    public void runReconfigure() throws Exception {
        this.managementSocket.send(this.pbSerializer.serialize(SetConfigMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setIsUpdate(true)
                .putConfig("key", "othervalue")
                .build()));
        Assert.assertArrayEquals(this.pbSerializer.serialize(ProcessStateUpdateMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setState(ProcessState.RUNNING)
                .setStateData(ByteString.EMPTY)
                .build()), this.managementSocket.read());
        Thread.sleep(100);
        Assert.assertEquals("modify", this.testService.getState());
    }

    public void runSuspend() throws Exception {
        this.managementSocket.send(this.pbSerializer.serialize(GoToProcessStateMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setTargetState(ProcessState.SUSPENDED)
                .build()));
        final byte[] barr = this.managementSocket.read();
        Assert.assertArrayEquals(this.pbSerializer.serialize(ProcessStateUpdateMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setState(ProcessState.SUSPENDED)
                .setStateData(ByteString.copyFrom(this.serializer.serialize(TestService.class)))
                .build()), barr);
        Thread.sleep(100);
        Assert.assertEquals("suspend", this.testService.getState());
    }

    public void runTerminate() throws Exception {
        this.managementSocket.send(this.pbSerializer.serialize(GoToProcessStateMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setTargetState(ProcessState.TERMINATED)
                .build()));
        Assert.assertArrayEquals(this.pbSerializer.serialize(ProcessStateUpdateMessage.newBuilder()
                .setProcessId(ServiceTest.PROCESS_ID)
                .setState(ProcessState.TERMINATED)
                .setStateData(ByteString.EMPTY)
                .build()), this.managementSocket.read());
        Thread.sleep(100);
        Assert.assertEquals("terminate", this.testService.getState());
    }

    @After
    public void stop() throws InterruptedException {
        if (this.manager != null) {
            this.manager.close();
            this.manager = null;
        }
        Thread.sleep(100);
    }

}
