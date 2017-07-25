/**
 * File ConnectionManager.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bson.types.ObjectId;
import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.exceptions.SerializationException;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.Connection;
import org.flexiblepower.model.Interface;
import org.flexiblepower.model.InterfaceVersion;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.Process.Parameter;
import org.flexiblepower.model.Process.ProcessState;
import org.flexiblepower.model.Service;
import org.flexiblepower.proto.ConnectionProto.ConnectionHandshake;
import org.flexiblepower.proto.ConnectionProto.ConnectionMessage;
import org.flexiblepower.proto.ServiceProto.GoToProcessStateMessage;
import org.flexiblepower.proto.ServiceProto.ProcessStateUpdateMessage;
import org.flexiblepower.proto.ServiceProto.ResumeProcessMessage;
import org.flexiblepower.proto.ServiceProto.SetConfigMessage;
import org.flexiblepower.proto.ServiceProto.SetConfigMessage.Builder;
import org.flexiblepower.serializers.ProtobufMessageSerializer;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

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

    synchronized static ProcessConnector getInstance() {
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
    public boolean addConnection(final Connection connection) throws ProcessNotFoundException {
        final Process process1 = ProcessManager.getInstance().getProcess(connection.getProcess1Id());
        final ProcessConnection pc1 = this.getProcessConnection(process1.getId());
        final Process process2 = ProcessManager.getInstance().getProcess(connection.getProcess2Id());
        final ProcessConnection pc2 = this.getProcessConnection(process2.getId());

        final Service service1 = ServiceManager.getInstance().getService(process1.getServiceId());
        final Service service2 = ServiceManager.getInstance().getService(process2.getServiceId());

        final Interface interface1 = service1.getInterface(connection.getInterface1Id());
        final Interface interface2 = service2.getInterface(connection.getInterface2Id());

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
                            process2.getId().toString(),
                            port2,
                            version2.getReceivesHash());

                    pc2.setUpConnection(connection.getId(),
                            port2,
                            version2.getSendsHash(),
                            process1.getId().toString(),
                            port1,
                            version1.getReceivesHash());

                    return true;
                }
            }
        }

        return false;
    }

    public void removeConnection(final Connection connection) {
        final Process process1 = ProcessManager.getInstance().getProcess(connection.getProcess1Id());
        final ProcessConnection pc1 = this.getProcessConnection(process1.getId());
        final Process process2 = ProcessManager.getInstance().getProcess(connection.getProcess2Id());
        final ProcessConnection pc2 = this.getProcessConnection(process2.getId());

        pc1.tearDownConnection(connection.getId());
        pc2.tearDownConnection(connection.getId());

    }

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

    /**
     * @param id
     * @param configuration
     * @return
     */
    public Process updateConfiguration(final ObjectId processId, final List<Parameter> configuration) {
        final Process process = MongoDbConnector.getInstance().get(Process.class, processId);
        process.setConfiguration(configuration);
        MongoDbConnector.getInstance().save(process);
        this.getProcessConnection(processId).updateConfiguration();
        return process;
    }

    private static final class ProcessConnection {

        private static int MANAGEMENT_SOCKET_SEND_TIMEOUT = 10000;
        private static int MANAGEMENT_SOCKET_RECV_TIMEOUT = 10000;
        private static int MANAGEMENT_PORT = 4999;

        private final ProtobufMessageSerializer serializer = new ProtobufMessageSerializer();
        private Socket socket = null;
        private final ObjectId processId;
        private String uri;
        private ByteString suspendState;

        public ProcessConnection(final ObjectId processId) {
            ProcessConnector.log.debug("Creating new ProcessConnection for process " + processId);
            this.processId = processId;
            this.serializer.addMessageClass(GoToProcessStateMessage.class);
            this.serializer.addMessageClass(ResumeProcessMessage.class);
            this.serializer.addMessageClass(ProcessStateUpdateMessage.class);
            this.serializer.addMessageClass(SetConfigMessage.class);
            this.serializer.addMessageClass(ConnectionHandshake.class);
            this.serializer.addMessageClass(ConnectionMessage.class);
            this.connectWithProcess();
        }

        public void connectWithProcess() {
            try {
                final Process process = ProcessManager.getInstance().getProcess(this.processId);
                if (process == null) {
                    throw new IllegalArgumentException(
                            "Provided ObjectId for Process " + this.processId + " does not exist");
                }
                this.uri = String.format("tcp://%s:%d", process.getId().toString(), ProcessConnection.MANAGEMENT_PORT);

                this.socket = ZMQ.context(1).socket(ZMQ.REQ);
                this.socket.setDelayAttachOnConnect(true);
                this.socket.connect(this.uri.toString());
                this.socket.setSendTimeOut(ProcessConnection.MANAGEMENT_SOCKET_SEND_TIMEOUT);
                this.socket.setReceiveTimeOut(ProcessConnection.MANAGEMENT_SOCKET_RECV_TIMEOUT);
                ProcessConnector.log.debug("Connecting with process on address " + this.uri);
            } catch (final Throwable t) {
                ProcessConnector.log.error("Could not connect with container ", t);
            }
        }

        public void setUpConnection(final ObjectId connectionId,
                final int listeningPort,
                final String sendingHash,
                final String receivingHost,
                final int targetPort,
                final String receivingHash) {
            final String targetAddress = "tcp://" + receivingHost + ":" + targetPort;

            final ConnectionMessage connectionMessage = ConnectionMessage.newBuilder()
                    .setConnectionId(connectionId.toString())
                    .setMode(ConnectionMessage.ModeType.CREATE)
                    .setTargetAddress(targetAddress)
                    .setListenPort(listeningPort)
                    .setReceiveHash(receivingHash)
                    .setSendHash(sendingHash)
                    .build();

            final ConnectionHandshake response = this.send(connectionMessage, ConnectionHandshake.class);
            if (response != null) {
                ProcessConnector.log
                        .debug("Connection " + connectionId + " status: " + response.getConnectionState().name());
            }
        }

        public void tearDownConnection(final ObjectId connectionId) {
            final ConnectionMessage connectionMessage = ConnectionMessage.newBuilder()
                    .setConnectionId(connectionId.toString())
                    .setMode(ConnectionMessage.ModeType.TERMINATE)
                    .build();

            final ConnectionHandshake response = this.send(connectionMessage, ConnectionHandshake.class);
            if (response != null) {
                ProcessConnector.log
                        .debug("Connection " + connectionId + " status: " + response.getConnectionState().name());
            }
        }

        public void startProcess() {
            final Process process = ProcessManager.getInstance().getProcess(this.processId);
            final Builder builder = SetConfigMessage.newBuilder().setProcessId(process.getId().toString()).setIsUpdate(
                    false);
            if (process.getConfiguration() != null) {
                for (final Parameter p : process.getConfiguration()) {
                    builder.putConfig(p.getKey(), p.getValue());
                }
            }
            final SetConfigMessage msg = builder.build();

            ProcessConnector.log.info("Starting process " + this.processId);

            final ProcessStateUpdateMessage response = this.send(msg, ProcessStateUpdateMessage.class);
            if (response != null) {
                this.updateProcessStateInDb(response.getState());
            }
        }

        public void resumeProcess() {
            final ResumeProcessMessage msg = ResumeProcessMessage.newBuilder()
                    .setProcessId(this.processId.toString())
                    .setStateData(this.suspendState)
                    .build();

            final ProcessStateUpdateMessage response = this.send(msg, ProcessStateUpdateMessage.class);
            if (response != null) {
                this.updateProcessStateInDb(response.getState());
            }
        }

        public void updateConfiguration() {
            final Process process = ProcessManager.getInstance().getProcess(this.processId);
            final Builder builder = SetConfigMessage.newBuilder().setProcessId(process.getId().toString()).setIsUpdate(
                    true);
            if (process.getConfiguration() != null) {
                for (final Parameter p : process.getConfiguration()) {
                    builder.putConfig(p.getKey(), p.getValue());
                }
            }
            final SetConfigMessage msg = builder.build();

            final ProcessStateUpdateMessage response = this.send(msg, ProcessStateUpdateMessage.class);
            if (response != null) {
                this.updateProcessStateInDb(response.getState());
            }
        }

        public void suspendProcess() {
            final GoToProcessStateMessage msg = GoToProcessStateMessage.newBuilder()
                    .setProcessId(this.processId.toString())
                    .setTargetState(org.flexiblepower.proto.ServiceProto.ProcessState.SUSPENDED)
                    .build();
            final ProcessStateUpdateMessage response = this.send(msg, ProcessStateUpdateMessage.class);

            if (response != null) {
                this.updateProcessStateInDb(response.getState());
                this.suspendState = response.getStateData();
                if (!response.getState().equals(org.flexiblepower.proto.ServiceProto.ProcessState.SUSPENDED)) {
                    ProcessConnector.log.error("Sended terminate insruction to Process " + this.processId.toString()
                            + ", but the process did not go to suspeded state.");
                }
            }
        }

        public void terminateProcess() {
            // Terminate process
            final GoToProcessStateMessage msg = GoToProcessStateMessage.newBuilder()
                    .setProcessId(this.processId.toString())
                    .setTargetState(org.flexiblepower.proto.ServiceProto.ProcessState.TERMINATED)
                    .build();
            final ProcessStateUpdateMessage response = this.send(msg, ProcessStateUpdateMessage.class);

            if (response != null) {
                this.updateProcessStateInDb(response.getState());
                if (!response.getState().equals(org.flexiblepower.proto.ServiceProto.ProcessState.TERMINATED)) {
                    ProcessConnector.log.error("Sended terminate insruction to Process " + this.processId.toString()
                            + ", but the process did not go to terminated state.");
                }
            }

            // Terminate connection with process
            this.socket.disconnect(this.uri);
            this.socket.close();
            ProcessConnector.getInstance().processConnectionTerminated(this.processId);
        }

        @SuppressWarnings("unchecked")
        private <T> T send(final Message msg, final Class<T> expected) {
            try {
                this.socket.send(this.serializer.serialize(msg));
            } catch (final SerializationException e1) {
                ProcessConnector.log.error("Could not serialize message", e1);
            }
            final byte[] recv = this.socket.recv();
            // TODO could be null?
            try {
                final Message m = this.serializer.deserialize(recv);
                if (expected.isInstance(m)) {
                    return (T) m;
                } else {
                    ProcessConnector.log.error("Received invalid message from Process " + this.processId.toString()
                            + ". Expected " + expected.getSimpleName() + ", got " + m.getClass().getSimpleName());
                    return null;
                }
            } catch (final SerializationException e) {
                ProcessConnector.log.error("Received invalid message from Process " + this.processId.toString()
                        + ". Expected " + expected.getSimpleName() + ".");
                return null;
            }
        }

        private void updateProcessStateInDb(final org.flexiblepower.proto.ServiceProto.ProcessState processState) {
            final Process.ProcessState state;
            switch (processState) {
            case INITIALIZING:
                state = ProcessState.INITIALIZING;
                break;
            case RUNNING:
                state = ProcessState.RUNNING;
                break;
            case STARTING:
                state = ProcessState.STARTING;
                break;
            case SUSPENDED:
                state = ProcessState.SUSPENDED;
                break;
            case TERMINATED:
                state = ProcessState.TERMINATED;
                break;
            default:
                state = null;
            }
            final MongoDbConnector mongoDbConnector = MongoDbConnector.getInstance();
            final Process process = mongoDbConnector.get(Process.class, this.processId);
            process.setState(state);
            mongoDbConnector.save(process);
        }
    }

}