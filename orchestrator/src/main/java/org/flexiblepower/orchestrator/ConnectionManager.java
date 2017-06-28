/**
 * File ConnectionManager.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator;

import java.io.IOException;
import java.util.Random;

import org.flexiblepower.commons.ConnectionResponseHandler;
import org.flexiblepower.exceptions.ConnectionException;
import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.exceptions.SerializationException;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.Connection;
import org.flexiblepower.model.Interface;
import org.flexiblepower.model.InterfaceVersion;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.Service;
import org.flexiblepower.proto.ConnectionProto.ConnectionHandshake;
import org.flexiblepower.proto.ConnectionProto.ConnectionMessage;
import org.flexiblepower.proto.ConnectionProto.ConnectionState;
import org.flexiblepower.serializers.ProtobufMessageSerializer;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

import lombok.extern.slf4j.Slf4j;

/**
 * ConnectionManager
 *
 * @author coenvl
 * @version 0.1
 * @since Apr 19, 2017
 */
@Slf4j
public class ConnectionManager {

    /**
     * Timeout of send/recv operations in milliseconds
     */
    private static int EXPECTED_STATE_TIMEOUT = 1000;
    private static int MANAGEMENT_SOCKET_SEND_TIMEOUT = 1000;
    private static int MANAGEMENT_SOCKET_RECV_TIMEOUT = 1000;
    private static int MANAGEMENT_PORT = 4999;

    private ProtobufMessageSerializer<ConnectionHandshake> serializer = null;

    public ConnectionManager() {
        this.serializer = new ProtobufMessageSerializer<>();
        this.serializer.addMessageClass(ConnectionHandshake.class);
    }

    /**
     * @param connection
     * @return
     * @throws ProcessNotFoundException
     * @throws IOException
     * @throws ServiceNotFoundException
     */
    public boolean addConnection(final Connection connection)
            throws ProcessNotFoundException, ConnectionException, ServiceNotFoundException {
        final Process process1 = ProcessManager.getInstance().getProcess(connection.getProcess1());
        final Process process2 = ProcessManager.getInstance().getProcess(connection.getProcess2());

        final Service service1 = ServiceManager.getInstance().getService(process1.getServiceId());
        final Service service2 = ServiceManager.getInstance().getService(process2.getServiceId());

        final Interface interface1 = service1.getInterface(connection.getInterface1());
        final Interface interface2 = service2.getInterface(connection.getInterface2());

        for (final InterfaceVersion version1 : interface1.getInterfaceVersions()) {
            for (final InterfaceVersion version2 : interface2.getInterfaceVersions()) {
                if (version1.getReceivesHash().equals(version2.getSendsHash())
                        && version2.getReceivesHash().equals(version1.getSendsHash())) {

                    final int port1 = 5000 + new Random().nextInt(5000);
                    final int port2 = 5000 + new Random().nextInt(5000);

                    this.connect(connection.getId().toString(),
                            process1.getRunningDockerNodeId(),
                            port1,
                            version1.getSendsHash(),
                            process2.getRunningDockerNodeId(),
                            port2,
                            version2.getReceivesHash());

                    this.connect(connection.getId().toString(),
                            process2.getRunningDockerNodeId(),
                            port2,
                            version2.getSendsHash(),
                            process1.getRunningDockerNodeId(),
                            port1,
                            version1.getReceivesHash());

                    return true;
                }
            }
        }

        return false;
    }

    public void connect(final String id,
            final String sendingHost,
            final int listeningPort,
            final String sendingHash,
            final String receivingHost,
            final int targetPort,
            final String receivingHash) throws ConnectionException {
        final String targetAddress = "tcp://" + receivingHost + ":" + targetPort;

        final ConnectionMessage connectionMessage = ConnectionMessage.newBuilder()
                .setConnectionId(id)
                .setMode(ConnectionMessage.ModeType.CREATE)
                .setTargetAddress(targetAddress)
                .setListenPort(listeningPort)
                .setReceiveHash(receivingHash)
                .setSendHash(sendingHash)
                .build();

        this.sendConnectionMessage(sendingHost, connectionMessage, new ConnectionResponseHandler() {

            @Override
            public void timeOutOccurred() throws ConnectionException {
                throw new ConnectionException("Timeout occurred waiting for " + this.expectedState().name());
            }

            @Override
            public void handleConnectionResponse(final ConnectionHandshake message) {
                ConnectionManager.log.debug("Connection " + id + " status: " + message.getConnectionState().name());
            }

            @Override
            public ConnectionState expectedState() {
                return ConnectionState.CONNECTED;
            }
        });
    }

    void disconnect(final String id, final String sendingHost) throws ConnectionException {
        final ConnectionMessage connectionMessage = ConnectionMessage.newBuilder()
                .setConnectionId(id)
                .setMode(ConnectionMessage.ModeType.TERMINATE)
                .build();

        this.sendConnectionMessage(sendingHost, connectionMessage, new ConnectionResponseHandler() {

            @Override
            public void timeOutOccurred() throws ConnectionException {
                throw new ConnectionException("Timeout occurred waiting for " + this.expectedState().name());
            }

            @Override
            public void handleConnectionResponse(final ConnectionHandshake message) {
                ConnectionManager.log.debug("Connection " + id + " status: " + message.getConnectionState().name());
            }

            @Override
            public ConnectionState expectedState() {
                return ConnectionState.TERMINATED;
            }
        });
    }

    /**
     * Send a message to the service on the provided IP address, telling him to start a new session with the provided
     * details.
     *
     * @param ip
     * @param session
     * @return
     */
    void sendConnectionMessage(final String ip,
            final ConnectionMessage session,
            final ConnectionResponseHandler handler) {

        final Thread connectionThread = new Thread(() -> {
            final String uri = String.format("tcp://%s:%d", ip, ConnectionManager.MANAGEMENT_PORT);
            boolean expectedMessageArrived = false;
            boolean timeOutOccurred = false;
            ConnectionManager.log.info("Sending session {} to {}", session, uri);

            try (Socket socket = ZMQ.context(1).socket(ZMQ.REQ)) {
                socket.setDelayAttachOnConnect(true);
                socket.connect(uri.toString());

                // This should work okay
                socket.setSendTimeOut(ConnectionManager.MANAGEMENT_SOCKET_SEND_TIMEOUT);
                socket.setReceiveTimeOut(ConnectionManager.MANAGEMENT_SOCKET_RECV_TIMEOUT);

                if (socket.send(session.toByteArray(), 0)) {
                    final long start = System.currentTimeMillis();
                    while (!expectedMessageArrived && !timeOutOccurred) {
                        final byte[] buffer = socket.recv();
                        if ((System.currentTimeMillis() - start) > ConnectionManager.EXPECTED_STATE_TIMEOUT) {
                            handler.timeOutOccurred();
                            timeOutOccurred = true;
                        }
                        if ((buffer != null) && (buffer.length != 0)) {
                            final ConnectionHandshake message = this.serializer.deserialize(buffer);
                            handler.handleConnectionResponse(message);
                            if (handler.expectedState().equals(message.getConnectionState())) {
                                expectedMessageArrived = true;
                            }
                        }
                    }
                }
            } catch (SerializationException | ConnectionException e) {
                e.printStackTrace();
            }
        });

        connectionThread.start();
    }

}
