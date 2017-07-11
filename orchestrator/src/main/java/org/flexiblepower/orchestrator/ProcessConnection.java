package org.flexiblepower.orchestrator;

import org.bson.types.ObjectId;
import org.flexiblepower.exceptions.ConnectionException;
import org.flexiblepower.exceptions.SerializationException;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.Process.Parameter;
import org.flexiblepower.model.Process.ProcessState;
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
 * ProcessConnection is responsible for maintaining a connection for the control-protocol between Orchestrator and a
 * single Process.
 */
@Slf4j
public class ProcessConnection {

    private static int MANAGEMENT_SOCKET_SEND_TIMEOUT = 10000;
    private static int MANAGEMENT_SOCKET_RECV_TIMEOUT = 10000;
    private static int MANAGEMENT_PORT = 4999;

    private final ProtobufMessageSerializer serializer = new ProtobufMessageSerializer();
    private Socket socket = null;
    private final ObjectId processId;
    private String uri;
    private ByteString suspendState;

    public ProcessConnection(final ObjectId processId) {
        ProcessConnection.log.debug("Creating new ProcessConnection for process " + processId);
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
            ProcessConnection.log.debug("Connecting with process on address " + this.uri);
        } catch (final Throwable t) {
            ProcessConnection.log.error("Could not connect with container ", t);
        }
    }

    public void setUpConnection(final ObjectId connectionId,
            final int listeningPort,
            final String sendingHash,
            final String receivingHost,
            final int targetPort,
            final String receivingHash) throws ConnectionException {
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
            ProcessConnection.log
                    .debug("Connection " + connectionId + " status: " + response.getConnectionState().name());
        }
    }

    public void tearDownConnection(final ObjectId connectionId) throws ConnectionException {
        final ConnectionMessage connectionMessage = ConnectionMessage.newBuilder()
                .setConnectionId(connectionId.toString())
                .setMode(ConnectionMessage.ModeType.TERMINATE)
                .build();

        final ConnectionHandshake response = this.send(connectionMessage, ConnectionHandshake.class);
        if (response != null) {
            ProcessConnection.log
                    .debug("Connection " + connectionId + " status: " + response.getConnectionState().name());
        }
    }

    public void startProcess() {
        final Process process = ProcessManager.getInstance().getProcess(this.processId);
        final Builder builder = SetConfigMessage.newBuilder()
                .setProcessId(process.getId().toString())
                .setIsUpdate(false);
        if (process.getConfiguration() != null) {
            for (final Parameter p : process.getConfiguration()) {
                builder.putConfig(p.getKey(), p.getValue());
            }
        }
        final SetConfigMessage msg = builder.build();

        ProcessConnection.log.info("Starting process " + this.processId);

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
        final Builder builder = SetConfigMessage.newBuilder()
                .setProcessId(process.getId().toString())
                .setIsUpdate(true);
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
                ProcessConnection.log.error("Sended terminate insruction to Process " + this.processId.toString()
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
                ProcessConnection.log.error("Sended terminate insruction to Process " + this.processId.toString()
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
            ProcessConnection.log.error("Could not serialize message", e1);
        }
        final byte[] recv = this.socket.recv();
        // TODO could be null?
        try {
            final Message m = this.serializer.deserialize(recv);
            if (expected.isInstance(m)) {
                return (T) m;
            } else {
                ProcessConnection.log.error("Received invalid message from Process " + this.processId.toString()
                        + ". Expected " + expected.getSimpleName() + ", got " + m.getClass().getSimpleName());
                return null;
            }
        } catch (final SerializationException e) {
            ProcessConnection.log.error("Received invalid message from Process " + this.processId.toString()
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
