/*-
 * #%L
 * dEF-Pi REST Orchestrator
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
package org.flexiblepower.connectors;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bson.types.ObjectId;
import org.flexiblepower.commons.TCPSocket;
import org.flexiblepower.exceptions.NotFoundException;
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
import org.flexiblepower.proto.ConnectionProto.ConnectionMessage.ModeType;
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
 * ProcessConnector
 *
 * @version 0.1
 * @since Apr 19, 2017
 */
public class ProcessConnector {

    /**
     * Log all relevant events for this class
     */
    protected final static Logger log = LoggerFactory.getLogger(ProcessConnector.class);

    private static ProcessConnector instance = null;

    private final Map<ObjectId, ProcessConnection> connections = new ConcurrentHashMap<>();

    private ProcessConnector() {
        // Private constructor for the singleton object
    }

    /**
     * @return The singleton instance of the ProcessConnector
     */
    public static ProcessConnector getInstance() {
        if (ProcessConnector.instance == null) {
            ProcessConnector.instance = new ProcessConnector();
        }
        return ProcessConnector.instance;
    }

    /**
     * Returns a process connection to the process, or null if it is unable to connect. This function uses a hashmap to
     * cache all process connections. If a connection is not yet present a new connection will be made and stored in the
     * hashmap.
     *
     * @param processId the ID of the process to get the connection to
     * @return The ProcessConnection connecting to the process or null if it was unable to connect
     * @throws ProcessNotFoundException When the process was not found by the ProcessManager
     * @see ProcessConnection
     */
    private ProcessConnection getProcessConnection(final ObjectId processId) throws ProcessNotFoundException {
        // Let's throw a message if the process is not present in DB
        ProcessConnector.log.debug("Fetching connection with process {}", processId);
        ProcessManager.getInstance().getProcess(processId);

        if (!this.connections.containsKey(processId)) {
            final ProcessConnection processConnection = new ProcessConnection(processId);
            if (processConnection.connectWithProcess()) {
                ProcessConnector.log.debug("Connected with process on address " + processId);
                this.connections.put(processId, processConnection);
            } else {
                ProcessConnector.log.debug("Unable to connect to process " + processId);
                return null;
            }
        }
        return this.connections.get(processId);
    }

    /**
     * Create a connection endpoint. This means the connection will be started at least at the provided endpoint. A
     * connection message will be sent to the management socket of the owning process to setup the connection.
     *
     * @param connection The connection which contains the endpoint to create
     * @param endpoint The endpoint to create
     * @return Whether the connection endpoint was successfully created or not
     * @throws ProcessNotFoundException When the process containing the endpoint cannot be found
     */
    public boolean createConnectionEndpoint(final Connection connection, final Endpoint endpoint)
            throws ProcessNotFoundException {
        final Process process = ProcessManager.getInstance().getProcess(endpoint.getProcessId());

        if (process.getState() != ProcessState.RUNNING) {
            ProcessConnector.log.warn("Not creating connection endpoint because process {} is not (yet) running",
                    process.getId());
            return false;
        }

        final ProcessConnection pc = this.getProcessConnection(process.getId());
        return (pc != null) && pc.setupConnectionEndpoint(connection, endpoint);
    }

    /**
     * Terminate a connection endpoint. This means the connection will be terminated, at least from the point of view
     * from the provided endpoint.
     *
     * @param connection The connection which contains the endpoint to terminate
     * @param endpoint The endpoint to terminate
     * @return Whether the connection endpoint was successfully terminated or not
     * @throws ProcessNotFoundException When the process containing the endpoint cannot be found
     */
    public boolean terminateConnectionEndpoint(final Connection connection, final Endpoint endpoint)
            throws ProcessNotFoundException {
        final ProcessConnection pc = this.getProcessConnection(endpoint.getProcessId());
        return (pc != null) && pc.tearDownConnection(connection.getId());
    }

    /**
     * Suspend a connection endpoint. This means the connection will be temporarily unavailable, at least from the point
     * of view from the provided endpoint.
     *
     * @param connection The connection which contains the endpoint to suspend
     * @param endpoint The endpoint to suspend
     * @return Whether the connection endpoint was successfully suspended or not
     * @throws ProcessNotFoundException When the process containing the endpoint cannot be found
     */
    public boolean suspendConnectionEndpoint(final Connection connection, final Endpoint endpoint)
            throws ProcessNotFoundException {
        final ProcessConnection pc = this.getProcessConnection(endpoint.getProcessId());
        return (pc != null) && pc.suspendConnection(connection.getId());
    }

    /**
     * Resume a connection endpoint. This means the connection will be re-established, at least from the side of the
     * provided endpoint.
     *
     * @param connection The connection which contains the endpoint to resume
     * @param endpoint The endpoint to resume
     * @return Whether the connection endpoint was successfully resumed or not
     * @throws ProcessNotFoundException When the process containing the endpoint cannot be found
     */
    public boolean resumeConnectionEndpoint(final Connection connection, final Endpoint endpoint)
            throws ProcessNotFoundException {
        final ProcessConnection pc = this.getProcessConnection(endpoint.getProcessId());
        return (pc != null) && pc.resumeConnectionEndpoint(connection, endpoint);
    }

    /**
     * When the ProcessConnection is terminated (i.e. when its close is called), it can be cleaned from the cache.
     *
     * @param processId The Id of the process that has terminated
     */
    void processConnectionTerminated(final ObjectId processId) {
        // Note that we do not need to close the connection here, instead it is the other way around.
        this.connections.remove(processId);
    }

    /**
     * Initialize a new process by sending its configuration.
     *
     * @param processId the ID of the process to initialize
     * @return Whether the process was successfully initialized or not
     * @throws ProcessNotFoundException If the process is not found by the ProcessManager
     * @see #terminate(ObjectId)
     */
    public boolean initNewProcess(final ObjectId processId) throws ProcessNotFoundException {
        final ProcessConnection processConnection = this.getProcessConnection(processId);
        return (processConnection != null) && processConnection.startProcess();
    }

    /**
     * Terminate a process "nicely", i.e. send him a Terminate signal.
     *
     * @param processId the ID of the process to terminate
     * @return Whether the process was successfully terminated or not
     * @throws ProcessNotFoundException If the process is not found by the ProcessManager
     * @see #initNewProcess(ObjectId)
     */
    public boolean terminate(final ObjectId processId) throws ProcessNotFoundException {
        final ProcessConnection processConnection = this.getProcessConnection(processId);
        return (processConnection != null) && processConnection.terminateProcess();
    }

    /**
     * Resume a process from the suspended state by sending a RESUME message
     *
     * @param processId the ID of the process to resume
     * @param suspendState The serialize state that the process should resume with
     * @return Whether the process was successfully resumed or not
     * @throws ProcessNotFoundException If the process is not found by the ProcessManager
     * @see #suspendProcess(ObjectId)
     */
    public boolean resume(final ObjectId processId, final byte[] suspendState) throws ProcessNotFoundException {
        final ProcessConnection processConnection = this.getProcessConnection(processId);
        return (processConnection != null) && processConnection.resumeProcess(suspendState);
    }

    /**
     * Suspend a process temporarily by sending a SUSPEND message
     *
     * @param processId the ID of the process to suspend
     * @return The serialized state that the process wants to be re-instantiated with when it will be resumed
     * @throws ProcessNotFoundException If the process is not found by the ProcessManager
     * @see #resume(ObjectId, byte[])
     */
    public byte[] suspendProcess(final ObjectId processId) throws ProcessNotFoundException {
        final ProcessConnection processConnection = this.getProcessConnection(processId);
        return processConnection == null ? null : processConnection.suspendProcess();
    }

    /**
     * Update the configuration of a running process by sending a configuration modification message.
     *
     * @param processId the ID of the process to modify
     * @param configuration A List of parameters that represent the updated configuration
     * @return Whether the process was successfully updated or not
     * @throws ProcessNotFoundException If the process is not found by the ProcessManager
     */
    public boolean updateConfiguration(final ObjectId processId, final List<ProcessParameter> configuration)
            throws ProcessNotFoundException {
        final ProcessConnection processConnection = this.getProcessConnection(processId);
        return (processConnection != null) && processConnection.updateConfiguration(configuration);
    }

    /**
     * Disconnect the connection with the remote process, and remove from the cache
     *
     * @param processId The ID of the process to disconnect with.
     */
    public void disconnect(final ObjectId processId) {
        final ProcessConnection processConnection = this.connections.get(processId);
        if (processConnection != null) {
            processConnection.close();
        }
    }

    private static final class ProcessConnection {

        private static final int IO_TIMEOUT = 10000;
        private static final int MANAGEMENT_PORT = 4999;

        private final ProtobufMessageSerializer serializer = new ProtobufMessageSerializer();
        private TCPSocket socket = null;
        private final ObjectId processId;

        ProcessConnection(final ObjectId processId) {
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

        synchronized boolean connectWithProcess() {
            try {
                final Process process = ProcessManager.getInstance().getProcess(this.processId);
                DockerConnector.getInstance().ensureProcessNetworkIsAttached(process);
                // if (process == null) {
                // throw new IllegalArgumentException(
                // "Provided ObjectId for Process " + this.processId + " does not exist");
                // }

                if (this.socket != null) {
                    this.socket.close();
                }

                this.socket = TCPSocket.asClient(process.getId().toString(), ProcessConnection.MANAGEMENT_PORT);
                return this.socket.waitUntilConnected(ProcessConnection.IO_TIMEOUT);
            } catch (final Exception e) {
                if (this.socket != null) {
                    this.socket.close();
                }

                ProcessConnector.log
                        .warn("Exception while connecting with container ({}): {}", e.getClass(), e.getMessage());
                ProcessConnector.log.trace(e.getMessage(), e);
                return false;
            }
        }

        boolean setupConnectionEndpoint(final Connection connection, final Endpoint endpoint) {
            return this.createOrResumeEndpoint(connection, endpoint, ModeType.CREATE);
        }

        boolean resumeConnectionEndpoint(final Connection connection, final Endpoint endpoint) {
            return this.createOrResumeEndpoint(connection, endpoint, ModeType.RESUME);
        }

        private boolean
                createOrResumeEndpoint(final Connection connection, final Endpoint endpoint, final ModeType type) {
            final Endpoint otherEndpoint = connection.getOtherEndpoint(endpoint);
            String remoteServiceId;
            try {
                final Process otherProcess = ProcessManager.getInstance().getProcess(otherEndpoint.getProcessId());
                remoteServiceId = otherProcess.getServiceId();
            } catch (final ProcessNotFoundException e) {
                remoteServiceId = null;
            }

            InterfaceVersion interfaceVersion;
            try {
                final Process process = ProcessManager.getInstance().getProcess(this.processId);
                final Service service = ServiceManager.getInstance().getService(process.getServiceId());
                final Interface intface = service.getInterface(endpoint.getInterfaceId());
                if (intface == null) {
                    throw new ServiceNotFoundException("Interface " + endpoint.getInterfaceId() + " not found");
                }
                interfaceVersion = intface.getInterfaceVersionByName(endpoint.getInterfaceVersionName());
            } catch (final NotFoundException e) {
                ProcessConnector.log.debug("Exception while preparing connection message: {}", e.getMessage());
                return false;
            }

            if (interfaceVersion == null) {
                ProcessConnector.log.debug("Unable to determine interface version fo create / resume");
                return false;
            }

            // Decide if this endpoint will be server or client
            final String targetAddress = (endpoint.getProcessId().compareTo(otherEndpoint.getProcessId()) > 0
                    ? otherEndpoint.getProcessId().toString()
                    : "");

            final ConnectionMessage connectionMessage = ConnectionMessage.newBuilder()
                    .setConnectionId(connection.getId().toString())
                    .setMode(type)
                    .setTargetAddress(targetAddress)
                    .setListenPort(connection.getPort())
                    .setReceiveHash(interfaceVersion.getReceivesHash())
                    .setSendHash(interfaceVersion.getSendsHash())
                    .setRemoteProcessId(otherEndpoint.getProcessId().toString())
                    .setRemoteInterfaceId(otherEndpoint.getInterfaceId())
                    .setRemoteServiceId(remoteServiceId)
                    .build();

            final ConnectionHandshake response = this.send(connectionMessage, ConnectionHandshake.class);
            if (response != null) {
                ProcessConnector.log
                        .debug("Connection " + connection.getId() + " status: " + response.getConnectionState().name());
                return true;
            } else {
                return false;
            }
        }

        boolean tearDownConnection(final ObjectId connectionId) {
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

        boolean suspendConnection(final ObjectId connectionId) {
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

        boolean startProcess() throws ProcessNotFoundException {
            final Process process = ProcessManager.getInstance().getProcess(this.processId);
            final SetConfigMessage msg = this.createSetConfigMessage(process.getConfiguration(), false);

            ProcessConnector.log.info("Starting process " + this.processId);

            final ProcessStateUpdateMessage response = this.send(msg, ProcessStateUpdateMessage.class);
            if (response == null) {
                return false;
            } else {
                this.updateProcessStateInDb(response.getState());
                return true;
            }
        }

        boolean resumeProcess(final byte[] suspendState) {
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

        boolean updateConfiguration(final List<ProcessParameter> newConfiguration) {
            final SetConfigMessage msg = this.createSetConfigMessage(newConfiguration, true);

            final ProcessStateUpdateMessage response = this.send(msg, ProcessStateUpdateMessage.class);
            if (response == null) {
                return false;
            } else {
                this.updateProcessStateInDb(response.getState());
                return true;
            }
        }

        byte[] suspendProcess() {
            byte[] suspendState; // = new byte[0];
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

        boolean terminateProcess() {
            // Terminate process
            final GoToProcessStateMessage msg = GoToProcessStateMessage.newBuilder()
                    .setProcessId(this.processId.toString())
                    .setTargetState(org.flexiblepower.proto.ServiceProto.ProcessState.TERMINATED)
                    .build();
            final ProcessStateUpdateMessage response = this.send(msg, ProcessStateUpdateMessage.class);

            if (response != null) {
                this.updateProcessStateInDb(response.getState());
                if (!response.getState().equals(org.flexiblepower.proto.ServiceProto.ProcessState.TERMINATED)) {
                    ProcessConnector.log.error("Sent terminate instruction to Process " + this.processId.toString()
                            + ", but the process did not go to terminated state.");
                }
            }

            this.close();

            return response != null;
        }

        synchronized void close() {
            ProcessConnector.log.debug("Terminating connection with process " + this.processId);
            // Terminate connection with process
            this.socket.close();
            ProcessConnector.getInstance().processConnectionTerminated(this.processId);
        }

        private SetConfigMessage createSetConfigMessage(final List<ProcessParameter> configuration,
                final boolean isUpdate) {
            final Builder builder = SetConfigMessage.newBuilder()
                    .setProcessId(this.processId.toString())
                    .setIsUpdate(isUpdate);

            // Set configuration
            if (configuration != null) {
                for (final ProcessParameter p : configuration) {
                    builder.putConfig(p.getKey(), p.getValue());
                }
            }

            return builder.build();
        }

        // Only one thread is allowed to do a send/receive at the time for each connection
        synchronized private <T> T send(final Message msg, final Class<T> expected) {
            byte[] data;
            try {
                data = this.serializer.serialize(msg);
            } catch (final SerializationException e) {
                ProcessConnector.log.error("Could not serialize message", e);
                return null;
            }

            try {
                this.socket.send(data);
            } catch (final IOException e) {
                ProcessConnector.log.warn("Exception while sending message to Process ({}), try to resend.",
                        e.getMessage());
                this.close();
                return null;
            }

            byte[] recv; // = null;
            try {
                recv = this.socket.read(ProcessConnection.IO_TIMEOUT);
            } catch (final IOException e) {
                ProcessConnector.log.warn("Exception while reading from socket ({}): {}", e.getClass(), e.getMessage());
                ProcessConnector.log.trace(e.getMessage(), e);
                this.close();
                return null;
            }

            if (recv == null) {
                ProcessConnector.log.warn("Did not receive a response from process, close and try again");
                this.close();
                return null;
            }

            try {
                final Message m = this.serializer.deserialize(recv);
                if (expected.isInstance(m)) {
                    @SuppressWarnings("unchecked")
                    final T ret = (T) m;
                    return ret;
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
