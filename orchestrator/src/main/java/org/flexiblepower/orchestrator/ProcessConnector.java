/**
 * File ConnectionManager.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.bson.types.ObjectId;
import org.flexiblepower.exceptions.ConnectionException;
import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.Connection;
import org.flexiblepower.model.Interface;
import org.flexiblepower.model.InterfaceVersion;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * ConnectionManager
 *
 * @author coenvl
 * @version 0.1
 * @since Apr 19, 2017
 */
@Slf4j
public class ProcessConnector {

    private static ProcessConnector instance = null;

    private final Map<ObjectId, ProcessConnection> connections = new HashMap<>();

    private ProcessConnector() {
    }

    public static ProcessConnector getInstance() {
        if (ProcessConnector.instance == null) {
            ProcessConnector.instance = new ProcessConnector();
        }
        return ProcessConnector.instance;
    }

    private ProcessConnection getProcessConnection(final ObjectId processId) {
        if (!this.connections.containsKey(processId)) {
            this.connections.put(processId, new ProcessConnection(processId));
        }
        return this.connections.get(processId);
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
        final ProcessConnection pc1 = this.getProcessConnection(process1.getId());
        final Process process2 = ProcessManager.getInstance().getProcess(connection.getProcess2());
        final ProcessConnection pc2 = this.getProcessConnection(process2.getId());

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
                    // TODO maybe random is not the best strategy?

                    pc1.setUpConnection(connection.getId(),
                            port1,
                            version1.getSendsHash(),
                            process2.getRunningDockerNodeId(),
                            port2,
                            version2.getReceivesHash());

                    pc2.setUpConnection(connection.getId(),
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

    // public void connect(final String id,
    // final String sendingHost,
    // final int listeningPort,
    // final String sendingHash,
    // final String receivingHost,
    // final int targetPort,
    // final String receivingHash) throws ConnectionException {
    // final String targetAddress = "tcp://" + receivingHost + ":" + targetPort;
    //
    // final ConnectionMessage connectionMessage = ConnectionMessage.newBuilder()
    // .setConnectionId(id)
    // .setMode(ConnectionMessage.ModeType.CREATE)
    // .setTargetAddress(targetAddress)
    // .setListenPort(listeningPort)
    // .setReceiveHash(receivingHash)
    // .setSendHash(sendingHash)
    // .build();
    //
    // this.sendConnectionMessage(sendingHost, connectionMessage, new ConnectionResponseHandler() {
    //
    // @Override
    // public void timeOutOccurred() throws ConnectionException {
    // throw new ConnectionException("Timeout occurred waiting for " + this.expectedState().name());
    // }
    //
    // @Override
    // public void handleConnectionResponse(final ConnectionHandshake message) {
    // ProcessConnector.log.debug("Connection " + id + " status: " + message.getConnectionState().name());
    // }
    //
    // @Override
    // public ConnectionState expectedState() {
    // return ConnectionState.CONNECTED;
    // }
    // });
    // }
    //
    // void disconnect(final String id, final String sendingHost) throws ConnectionException {
    // final ConnectionMessage connectionMessage = ConnectionMessage.newBuilder()
    // .setConnectionId(id)
    // .setMode(ConnectionMessage.ModeType.TERMINATE)
    // .build();
    //
    // this.sendConnectionMessage(sendingHost, connectionMessage, new ConnectionResponseHandler() {
    //
    // @Override
    // public void timeOutOccurred() throws ConnectionException {
    // throw new ConnectionException("Timeout occurred waiting for " + this.expectedState().name());
    // }
    //
    // @Override
    // public void handleConnectionResponse(final ConnectionHandshake message) {
    // ProcessConnector.log.debug("Connection " + id + " status: " + message.getConnectionState().name());
    // }
    //
    // @Override
    // public ConnectionState expectedState() {
    // return ConnectionState.TERMINATED;
    // }
    // });
    // }

    // /**
    // * Send a message to the service on the provided IP address, telling him to start a new session with the provided
    // * details.
    // *
    // * @param ip
    // * @param session
    // * @return
    // */
    // void sendConnectionMessage(final String ip,
    // final ConnectionMessage session,
    // final ConnectionResponseHandler handler) {
    //
    // final Thread connectionThread = new Thread(() -> {
    // final String uri = String.format("tcp://%s:%d", ip, ProcessConnector.MANAGEMENT_PORT);
    // boolean expectedMessageArrived = false;
    // boolean timeOutOccurred = false;
    // ProcessConnector.log.info("Sending session {} to {}", session, uri);
    //
    // try (Socket socket = ZMQ.context(1).socket(ZMQ.REQ)) {
    // socket.setDelayAttachOnConnect(true);
    // socket.connect(uri.toString());
    //
    // // This should work okay
    // socket.setSendTimeOut(ProcessConnector.MANAGEMENT_SOCKET_SEND_TIMEOUT);
    // socket.setReceiveTimeOut(ProcessConnector.MANAGEMENT_SOCKET_RECV_TIMEOUT);
    //
    // if (socket.send(session.toByteArray(), 0)) {
    // final long start = System.currentTimeMillis();
    // while (!expectedMessageArrived && !timeOutOccurred) {
    // final byte[] buffer = socket.recv();
    // if ((System.currentTimeMillis() - start) > ProcessConnector.EXPECTED_STATE_TIMEOUT) {
    // handler.timeOutOccurred();
    // timeOutOccurred = true;
    // }
    // if ((buffer != null) && (buffer.length != 0)) {
    // final ConnectionHandshake message = this.serializer.deserialize(buffer);
    // handler.handleConnectionResponse(message);
    // if (handler.expectedState().equals(message.getConnectionState())) {
    // expectedMessageArrived = true;
    // }
    // }
    // }
    // }
    // } catch (SerializationException | ConnectionException e) {
    // e.printStackTrace();
    // }
    // });
    //
    // connectionThread.start();
    // }

    public void processConnectionTerminated(final ObjectId processId) {
        this.connections.remove(processId);
    }

    /**
     * @param id
     */
    public void initNewProcess(final ObjectId processId) {
        final ProcessConnection processConnection = this.getProcessConnection(processId);
        processConnection.startProcess();
    }

    /**
     * @param id
     */
    public void terminate(final ObjectId processId) {
        final ProcessConnection processConnection = this.getProcessConnection(processId);
        processConnection.terminateProcess();
    }

}
