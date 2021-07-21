/**
 * File SerializationTest.java
 *
 * Copyright 2021 FAN
 */
package org.flexiblepower.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.flexiblepower.commons.TCPSocket;
import org.flexiblepower.exceptions.SerializationException;
import org.flexiblepower.proto.ConnectionProto.ConnectionMessage;
import org.flexiblepower.proto.ConnectionProto.ConnectionMessage.ModeType;
import org.flexiblepower.proto.ServiceProto.ProcessState;
import org.flexiblepower.proto.ServiceProto.ProcessStateUpdateMessage;
import org.flexiblepower.serializers.ProtobufMessageSerializer;
import org.junit.jupiter.api.Test;

/**
 * SerializationTest
 *
 * @version 0.1
 * @since Jul 6, 2021
 */
public class SerializationTest {

    private static ExecutorService executor = Executors.newSingleThreadExecutor();

    @Test
    public void runTest() throws Exception {
        try (
                final TCPSocket socket = TCPSocket.asServer(1234);
                final Socket plainOldCLient = new Socket("127.0.0.1", 1234)) {

            SerializationTest.executor.execute(() -> SerializationTest.drainSocket(plainOldCLient));
            socket.send(new String("Hello dEF-Pi!").getBytes());

            Thread.sleep(100);
            System.out.println("");
            socket.send(SerializationTest.startProcessMessage());
            Thread.sleep(100);
            System.out.println("");
            socket.send(SerializationTest.openConnection());
            Thread.sleep(100);
        }
    }

    private static byte[] startProcessMessage() throws SerializationException {
        final ProtobufMessageSerializer serializer = new ProtobufMessageSerializer();
        serializer.addMessageClass(ProcessStateUpdateMessage.class);

        return serializer.serialize(
                ProcessStateUpdateMessage.newBuilder().setProcessId("1337").setState(ProcessState.STARTING).build());
    }

    private static byte[] openConnection() throws SerializationException {
        final ProtobufMessageSerializer serializer = new ProtobufMessageSerializer();
        serializer.addMessageClass(ConnectionMessage.class);

        return serializer.serialize(ConnectionMessage.newBuilder()
                .setConnectionId("428")
                .setMode(ModeType.CREATE)
                .setListenPort(8779)
                .setTargetAddress("10.130.7.1")
                .build());
    }

    private static void drainSocket(final Socket socket) {
        try (final InputStream is = socket.getInputStream()) {
            int recv = 0;
            while (recv != -1) {
                recv = is.read();
                System.out.print(String.format("0x%02x, ", recv));
            }
        } catch (final IOException e) {
            // ignore
        }
    }

}
