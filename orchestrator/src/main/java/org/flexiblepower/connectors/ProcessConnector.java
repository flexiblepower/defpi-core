/**
 * File ConnectionManager.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.connectors;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.bson.types.ObjectId;
import org.flexiblepower.commons.TCPSocket;
import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.exceptions.SerializationException;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.Connection;
import org.flexiblepower.model.Connection.Endpoint;
import org.flexiblepower.model.Interface;
import org.flexiblepower.model.InterfaceVersion;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.Process.ProcessParameter;
import org.flexiblepower.model.Process.ProcessState;
import org.flexiblepower.model.Service;
import org.flexiblepower.orchestrator.ServiceManager;
import org.flexiblepower.process.ProcessManager;
import org.flexiblepower.proto.ConnectionProto.ConnectionHandshake;
import org.flexiblepower.proto.ConnectionProto.ConnectionMessage;
import org.flexiblepower.proto.ServiceProto.ErrorMessage;
import org.flexiblepower.proto.ServiceProto.GoToProcessStateMessage;
import org.flexiblepower.proto.ServiceProto.ProcessStateUpdateMessage;
import org.flexiblepower.proto.ServiceProto.ResumeProcessMessage;
import org.flexiblepower.proto.ServiceProto.SetConfigMessage;
import org.flexiblepower.proto.ServiceProto.SetConfigMessage.Builder;
import org.flexiblepower.serializers.ProtobufMessageSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

/**
 * ConnectionManager
 *
 * @author coenvl
 * @version 0.1
 * @since Apr 19, 2017
 */
public class ProcessConnector {

    protected final static Logger log = LoggerFactory.getLogger(ProcessConnector.class);

    private static ProcessConnector instance = null;

    private final Map<ObjectId, ProcessConnection> connections = new ConcurrentHashMap<>();

    private ProcessConnector() {
    }

    public synchronized static ProcessConnector getInstance() {
        if (ProcessConnector.instance == null) {
            ProcessConnector.instance = new ProcessConnector();
        }
        return ProcessConnector.instance;
    }

    private synchronized ProcessConnection getProcessConnection(final ObjectId processId)
            throws ProcessNotFoundException {
        // Let's throw a message if the process is not present in DB
        ProcessManager.getInstance().getProcess(processId);

        if (!this.connections.containsKey(processId)) {
            final ProcessConnection processConnection = new ProcessConnection(processId);
            if (processConnection.connectWithProcess()) {
                this.connections.put(processId, processConnection);
            } else {
                return null;
            }
        }
        return this.connections.get(processId);
    }

    public boolean createConnectionEndpoint(final Connection connection, final Endpoint endpoint)
            throws ProcessNotFoundException,
            ServiceNotFoundException {
        final Endpoint otherEndpoint = connection.getOtherEndpoint(endpoint);
        final Process process = ProcessManager.getInstance().getProcess(endpoint.getProcessId());

        if (process.getState() != ProcessState.RUNNING) {
            ProcessConnector.log.warn("Not creating connection endpoint because process {} is not (yet) running",
                    process.getId());
            return false;
        }

        final ProcessConnection pc = this.getProcessConnection(process.getId());
        if (pc == null) {
            ProcessConnector.log.warn("Unable to connect to process {}, not creating connection endpoint",
                    process.getId());
            return false;
        }

        final Service service = ServiceManager.getInstance().getService(process.getServiceId());

        final Interface intface = service.getInterface(endpoint.getInterfaceId());
        final InterfaceVersion interfaceVersion = intface.getInterfaceVersionByName(endpoint.getInterfaceVersionName());

        // Decide if this endpoint will be server or client
        final String targetAddress = (endpoint.getProcessId().compareTo(otherEndpoint.getProcessId()) > 0
                ? otherEndpoint.getProcessId().toString()
                : "");

        return pc.setUpConnection(connection.getId(),
                endpoint.getListenPort(),
                interfaceVersion.getSendsHash(),
                targetAddress,
                interfaceVersion.getReceivesHash());

    }

    public boolean terminateConnectionEndpoint(final Connection connection, final Endpoint endpoint)
            throws ProcessNotFoundException {
        final Process process = ProcessManager.getInstance().getProcess(endpoint.getProcessId());
        final ProcessConnection pc = this.getProcessConnection(process.getId());
        return pc == null ? false : pc.tearDownConnection(connection.getId());
    }

    /**
     * @param c
     * @throws ProcessNotFoundException
     */
    public boolean suspendConnectionEndpoint(final Connection connection, final Endpoint endpoint)
            throws ProcessNotFoundException {
        final Process process = ProcessManager.getInstance().getProcess(endpoint.getProcessId());
        final ProcessConnection pc = this.getProcessConnection(process.getId());
        return pc == null ? false : pc.suspendConnection(connection.getId());
    }

    /**
     * @param c
     * @throws ServiceNotFoundException
     * @throws ProcessNotFoundException
     */
    public boolean resumeConnectionEndpoint(final Connection connection, final Endpoint endpoint)
            throws ServiceNotFoundException,
            ProcessNotFoundException {
        final Endpoint otherEndpoint = connection.getOtherEndpoint(endpoint);
        final Process process = ProcessManager.getInstance().getProcess(endpoint.getProcessId());

        final ProcessConnection pc = this.getProcessConnection(process.getId());
        if (pc == null) {
            return false;
        }

        final Service service = ServiceManager.getInstance().getService(process.getServiceId());

        final Interface intface = service.getInterface(connection.getEndpoint1().getInterfaceId());
        final InterfaceVersion interfaceVersion = intface.getInterfaceVersionByName(endpoint.getInterfaceVersionName());

        return pc.resumeConnection(connection.getId(),
                endpoint.getListenPort(),
                interfaceVersion.getSendsHash(),
                otherEndpoint.getProcessId().toString(),
                otherEndpoint.getListenPort(),
                interfaceVersion.getReceivesHash());

    }

    public void processConnectionTerminated(final ObjectId processId) {
        this.connections.remove(processId);
    }

    /**
     * @param id
     * @throws ProcessNotFoundException
     */
    public boolean initNewProcess(final ObjectId processId) throws ProcessNotFoundException {
        final ProcessConnection processConnection = this.getProcessConnection(processId);
        return processConnection == null ? false : processConnection.startProcess();
    }

    /**
     * @param id
     * @throws ProcessNotFoundException
     */
    public boolean terminate(final ObjectId processId) throws ProcessNotFoundException {
        final ProcessConnection processConnection = this.getProcessConnection(processId);
        return processConnection == null ? false : processConnection.terminateProcess();
    }

    /**
     * @param id
     * @param suspendState
     * @throws ProcessNotFoundException
     */
    public boolean resume(final ObjectId processId, final byte[] suspendState) throws ProcessNotFoundException {
        final ProcessConnection processConnection = this.getProcessConnection(processId);
        return processConnection == null ? false : processConnection.resumeProcess(suspendState);
    }

    public byte[] suspendProcess(final ObjectId processId) throws ProcessNotFoundException {
        final ProcessConnection processConnection = this.getProcessConnection(processId);
        return processConnection == null ? null : processConnection.suspendProcess();
    }

    /**
     * @param id
     * @param configuration
     * @return
     * @throws ProcessNotFoundException
     */
    public boolean updateConfiguration(final ObjectId processId, final List<ProcessParameter> configuration)
            throws ProcessNotFoundException {
        final ProcessConnection processConnection = this.getProcessConnection(processId);
        return processConnection == null ? false : processConnection.updateConfiguration(configuration);
    }

    private static final class ProcessConnection {

        private static final long RETRY_TIMEOUT = 1000;
        private static final int MANAGEMENT_SOCKET_CONNECT_TIMEOUT = 10000;
        private static final int MANAGEMENT_PORT = 4999;

        private final ProtobufMessageSerializer serializer = new ProtobufMessageSerializer();
        private TCPSocket socket = null;
        private final ObjectId processId;

        public ProcessConnection(final ObjectId processId) {
            ProcessConnector.log.debug("Creating new ProcessConnection for process " + processId);
            this.processId = processId;
            this.serializer.addMessageClass(GoToProcessStateMessage.class);
            this.serializer.addMessageClass(ResumeProcessMessage.class);
            this.serializer.addMessageClass(ProcessStateUpdateMessage.class);
            this.serializer.addMessageClass(SetConfigMessage.class);
            this.serializer.addMessageClass(ConnectionHandshake.class);
            this.serializer.addMessageClass(ConnectionMessage.class);
            this.serializer.addMessageClass(ErrorMessage.class);
        }

        public boolean connectWithProcess() {
            try {
                final Process process = ProcessManager.getInstance().getProcess(this.processId);
                DockerConnector.getInstance().ensureProcessNetworkIsAttached(process);
                if (process == null) {
                    throw new IllegalArgumentException(
                            "Provided ObjectId for Process " + this.processId + " does not exist");
                }

                if (this.socket != null) {
                    this.socket.close();
                }
                this.socket = TCPSocket.asClient(process.getId().toString(), ProcessConnection.MANAGEMENT_PORT);
                this.socket.waitUntilConnected(ProcessConnection.MANAGEMENT_SOCKET_CONNECT_TIMEOUT);
                ProcessConnector.log.debug("Connected with process on address " + process.getId());
                return true;
            } catch (final Exception t) {
                if (this.socket != null) {
                    this.socket.close();
                }

                ProcessConnector.log.error("Could not connect with container");
                ProcessConnector.log.trace("Could not connect with container ", t);
                return false;
            }
        }

        public boolean setUpConnection(final ObjectId connectionId,
                final int listeningPort,
                final String sendsHash,
                final String targetAddress,
                final String receivesHash) {
            final ConnectionMessage connectionMessage = ConnectionMessage.newBuilder()
                    .setConnectionId(connectionId.toString())
                    .setMode(ConnectionMessage.ModeType.CREATE)
                    .setTargetAddress(targetAddress)
                    .setListenPort(listeningPort)
                    .setReceiveHash(receivesHash)
                    .setSendHash(sendsHash)
                    .build();

            final ConnectionHandshake response = this.send(connectionMessage, ConnectionHandshake.class);
            if (response != null) {
                ProcessConnector.log
                        .debug("Connection " + connectionId + " status: " + response.getConnectionState().name());
                return true;
            } else {
                return false;
            }
        }

        public boolean tearDownConnection(final ObjectId connectionId) {
            final ConnectionMessage connectionMessage = ConnectionMessage.newBuilder()
                    .setConnectionId(connectionId.toString())
                    .setMode(ConnectionMessage.ModeType.TERMINATE)
                    .build();

            final ConnectionHandshake response = this.send(connectionMessage, ConnectionHandshake.class);
            if (response != null) {
                ProcessConnector.log
                        .debug("Connection " + connectionId + " status: " + response.getConnectionState().name());
                return true;
            } else {
                return false;
            }
        }

        public boolean suspendConnection(final ObjectId connectionId) {
            final ConnectionMessage connectionMessage = ConnectionMessage.newBuilder()
                    .setConnectionId(connectionId.toString())
                    .setMode(ConnectionMessage.ModeType.SUSPEND)
                    .build();

            final ConnectionHandshake response = this.send(connectionMessage, ConnectionHandshake.class);
            if (response != null) {
                ProcessConnector.log
                        .debug("Connection " + connectionId + " status: " + response.getConnectionState().name());
                return true;
            } else {
                return false;
            }
        }

        public boolean resumeConnection(final ObjectId connectionId,
                final int listeningPort,
                final String sendingHash,
                final String receivingHost,
                final int targetPort,
                final String receivingHash) {
            final String targetAddress = "tcp://" + receivingHost + ":" + targetPort;

            final ConnectionMessage connectionMessage = ConnectionMessage.newBuilder()
                    .setConnectionId(connectionId.toString())
                    .setMode(ConnectionMessage.ModeType.RESUME)
                    .setTargetAddress(targetAddress)
                    .setListenPort(listeningPort)
                    .setReceiveHash(receivingHash)
                    .setSendHash(sendingHash)
                    .build();

            final ConnectionHandshake response = this.send(connectionMessage, ConnectionHandshake.class);
            if (response != null) {
                ProcessConnector.log
                        .debug("Connection " + connectionId + " status: " + response.getConnectionState().name());
                return true;
            } else {
                return false;
            }
        }

        public boolean startProcess() throws ProcessNotFoundException {
            final Process process = ProcessManager.getInstance().getProcess(this.processId);
            final Builder builder = SetConfigMessage.newBuilder().setProcessId(process.getId().toString()).setIsUpdate(
                    false);
            if (process.getConfiguration() != null) {
                for (final ProcessParameter p : process.getConfiguration()) {
                    builder.putConfig(p.getKey(), p.getValue());
                }
            }
            final SetConfigMessage msg = builder.build();

            ProcessConnector.log.info("Starting process " + this.processId);

            final ProcessStateUpdateMessage response = this.send(msg, ProcessStateUpdateMessage.class);
            if (response == null) {
                return false;
            } else {
                this.updateProcessStateInDb(response.getState());
                return true;
            }
        }

        public boolean resumeProcess(final byte[] suspendState) {
            final ResumeProcessMessage msg = ResumeProcessMessage.newBuilder()
                    .setProcessId(this.processId.toString())
                    .setStateData(suspendState == null ? ByteString.EMPTY : ByteString.copyFrom(suspendState))
                    .build();

            final ProcessStateUpdateMessage response = this.send(msg, ProcessStateUpdateMessage.class);
            if (response != null) {
                this.updateProcessStateInDb(response.getState());
                return true;
            } else {
                return false;
            }
        }

        /**
         * @param newConfiguration
         * @return true if successful, false in failed
         */
        public boolean updateConfiguration(final List<ProcessParameter> newConfiguration) {
            final Builder builder = SetConfigMessage.newBuilder().setProcessId(this.processId.toString()).setIsUpdate(
                    true);
            for (final ProcessParameter p : newConfiguration) {
                builder.putConfig(p.getKey(), p.getValue());
            }
            final SetConfigMessage msg = builder.build();

            final ProcessStateUpdateMessage response = this.send(msg, ProcessStateUpdateMessage.class);
            if (response == null) {
                return false;
            } else {
                this.updateProcessStateInDb(response.getState());
                return true;
            }
        }

        public byte[] suspendProcess() {
            byte[] suspendState = new byte[0];
            final GoToProcessStateMessage msg = GoToProcessStateMessage.newBuilder()
                    .setProcessId(this.processId.toString())
                    .setTargetState(org.flexiblepower.proto.ServiceProto.ProcessState.SUSPENDED)
                    .build();
            final ProcessStateUpdateMessage response = this.send(msg, ProcessStateUpdateMessage.class);

            if (response != null) {
                this.updateProcessStateInDb(response.getState());
                suspendState = response.getStateData().toByteArray();
                if (!response.getState().equals(org.flexiblepower.proto.ServiceProto.ProcessState.SUSPENDED)) {
                    ProcessConnector.log.error("Sended suspend instruction to Process " + this.processId.toString()
                            + ", but the process did not go to suspeded state.");
                }
            } else {
                return null;
            }

            this.close();

            return suspendState;
        }

        public boolean terminateProcess() {
            // Terminate process
            final GoToProcessStateMessage msg = GoToProcessStateMessage.newBuilder()
                    .setProcessId(this.processId.toString())
                    .setTargetState(org.flexiblepower.proto.ServiceProto.ProcessState.TERMINATED)
                    .build();
            final ProcessStateUpdateMessage response = this.send(msg, ProcessStateUpdateMessage.class);

            if (response != null) {
                this.updateProcessStateInDb(response.getState());
                if (!response.getState().equals(org.flexiblepower.proto.ServiceProto.ProcessState.TERMINATED)) {
                    ProcessConnector.log.error("Sent terminate insruction to Process " + this.processId.toString()
                            + ", but the process did not go to terminated state.");
                }
            }

            this.close();

            return response != null;
        }

        void close() {
            ProcessConnector.log.debug("Terminating connection with process " + this.processId);
            // Terminate connection with process
            this.socket.close();
            ProcessConnector.getInstance().processConnectionTerminated(this.processId);
        }

        @SuppressWarnings("unchecked")
        private <T> T send(final Message msg, final Class<T> expected) {
            // Only one threat is allowed to do a send/receive at the time for each connection
            synchronized (this) {
                byte[] data;
                try {
                    data = this.serializer.serialize(msg);
                } catch (final SerializationException e) {
                    ProcessConnector.log.error("Could not serialize message", e);
                    return null;
                }

                try {
                    this.socket.send(data, 1000);
                } catch (final Exception e) {
                    ProcessConnector.log.warn("Exception while sending message to Process ({}), try to resend.",
                            e.getMessage());
                    return null;
                }

                byte[] recv = null;
                try {
                    recv = this.socket.read(1000);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    ProcessConnector.log.warn("Exception while reading from socket {}", e.getMessage());
                    ProcessConnector.log.trace(e.getMessage(), e);
                    this.close();
                    return null;
                }

                try {
                    final Message m = this.serializer.deserialize(recv);
                    if (expected.isInstance(m)) {
                        return (T) m;
                    } else if (m instanceof ErrorMessage) {
                        ProcessConnector.log.error("Received Error message from Process " + this.processId.toString()
                                + ". Expected " + expected.getSimpleName() + ". Message: "
                                + ((ErrorMessage) m).getDebugInformation());
                        return null;
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

    /**
     * @param id
     */
    public void disconnect(final ObjectId processId) {
        final ProcessConnection processConnection = this.connections.get(processId);
        if (processConnection != null) {
            processConnection.close();
        }
    }

}
