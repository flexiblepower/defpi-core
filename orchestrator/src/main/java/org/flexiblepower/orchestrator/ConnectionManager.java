/**
 * File ConnectionManager.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator;

import java.io.IOException;
import java.util.Random;
import java.util.UUID;

import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.model.Connection;
import org.flexiblepower.model.Interface;
import org.flexiblepower.model.Process;
import org.flexiblepower.proto.ServiceProto.ConnectionMessage;
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
    private static final int MANAGEMENT_SOCKET_SEND_TIMEOUT = 2000;
    private static final int MANAGEMENT_SOCKET_RECV_TIMEOUT = 2000;
    private static final int MANAGEMENT_PORT = 4999;

    private final DockerConnector docker = new DockerConnector();
    private final RegistryConnector registry = new RegistryConnector();

    /**
     * @param connection
     * @return
     * @throws ProcessNotFoundException
     * @throws IOException
     */
    public void addConnection(final Connection connection) throws ProcessNotFoundException, IOException {
        // TODO Auto-generated method stub
        final Process process1 = this.docker.getProcess(connection.getProcess1());
        final Process process2 = this.docker.getProcess(connection.getProcess2());

        final Interface interface1 = this.registry.getInterface(connection.getInterface1());
        final Interface interface2 = this.registry.getInterface(connection.getInterface2());

        if (interface1.getPublishHash().equals(interface2.getSubscribeHash())) {
            ConnectionManager.connect(process1, interface1, process2, interface2);
        }

        if (interface2.getPublishHash().equals(interface1.getSubscribeHash())) {
            ConnectionManager.connect(process2, interface2, process1, interface1);
        }
    }

    /**
     * @param process1
     * @param interface1
     * @param process2
     * @param interface2
     * @throws IOException
     */
    static void connect(final Process processFrom,
            final Interface interfaceFrom,
            final Process processTo,
            final Interface interfaceTo) throws IOException {

        final int port1 = 5000 + new Random().nextInt(5000);
        final int port2 = 5000 + new Random().nextInt(5000);
        final String address1 = "tcp://" + processFrom.getRunningNode().getHostname() + ":" + port1;
        final String address2 = "tcp://" + processTo.getRunningNode().getHostname() + ":" + port2;

        final UUID id = UUID.randomUUID();

        final ConnectionMessage connection = ConnectionMessage.newBuilder()
                .setConnectionId(id.toString())
                .setMode(ConnectionMessage.ModeType.CREATE)
                .setTargetAddress(address2)
                .setListenPort(port1)
                .setReceiveHash(interfaceFrom.getPublishHash())
                .setSendHash(interfaceTo.getSubscribeHash())
                .build();

        if (ConnectionManager.sendStartSession(processFrom.getRunningNode().getHostname(), connection) != 0) {
            throw new IOException("No response received from client");
        }
    }

    /**
     * Send a message to the service on the provided IP address, telling him to start a new session with the provided
     * details.
     *
     * @param ip
     * @param session
     * @return
     */
    static int sendStartSession(final String ip, final ConnectionMessage session) {
        final String uri = String.format("tcp://%s:%d", ip, ConnectionManager.MANAGEMENT_PORT);
        ConnectionManager.log.info("Sending session {} to {}", session, uri);

        try (final Socket socket = ZMQ.context(1).socket(ZMQ.REQ)) {
            socket.connect(uri.toString());

            // This should work okay
            socket.setSendTimeOut(ConnectionManager.MANAGEMENT_SOCKET_SEND_TIMEOUT);
            socket.setReceiveTimeOut(ConnectionManager.MANAGEMENT_SOCKET_RECV_TIMEOUT);

            if (socket.send(session.toByteArray(), 0)) {
                final byte[] buffer = socket.recv();
                return (buffer == null ? -1 : buffer[0]);
            }
            return -1;
        }
    }

}
