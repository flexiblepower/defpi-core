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
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.Connection;
import org.flexiblepower.model.Interface;
import org.flexiblepower.model.InterfaceVersion;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.Service;
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

    private static ConnectionManager instance = null;

    private ConnectionManager() {

    }

    public static ConnectionManager getInstance() {
        if (ConnectionManager.instance == null) {
            ConnectionManager.instance = new ConnectionManager();
        }
        return ConnectionManager.instance;
    }

    /**
     * @param connection
     * @return
     * @throws ProcessNotFoundException
     * @throws IOException
     * @throws ServiceNotFoundException
     */
    public void addConnection(final Connection connection)
            throws ProcessNotFoundException, IOException, ServiceNotFoundException {
        // TODO Auto-generated method stub
        final Process process1 = this.docker.getProcess(connection.getProcess1());
        final Process process2 = this.docker.getProcess(connection.getProcess2());

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

                    ConnectionManager.connect(process1.getRunningNode(),
                            port1,
                            version1.getSendsHash(),
                            process2.getRunningNode(),
                            port2,
                            version2.getReceivesHash());

                    ConnectionManager.connect(process2.getRunningNode(),
                            port2,
                            version2.getSendsHash(),
                            process1.getRunningNode(),
                            port1,
                            version1.getReceivesHash());
                }
            }
        }
    }

    static void connect(final String sendingHost,
            final int listeningPort,
            final String sendingHash,
            final String receivingHost,
            final int targetPort,
            final String receivingHash) throws IOException {

        final String targetAddress = "tcp://" + receivingHost + ":" + targetPort;

        final UUID id = UUID.randomUUID();

        final ConnectionMessage connection = ConnectionMessage.newBuilder()
                .setConnectionId(id.toString())
                .setMode(ConnectionMessage.ModeType.CREATE)
                .setTargetAddress(targetAddress)
                .setListenPort(listeningPort)
                .setReceiveHash(receivingHash)
                .setSendHash(sendingHash)
                .build();

        if (ConnectionManager.sendStartSession(sendingHost, connection) != 0) {
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
