/**
 * File MoveProcess.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.process;

import java.util.List;

import org.bson.types.ObjectId;
import org.flexiblepower.connectors.DockerConnector;
import org.flexiblepower.connectors.MongoDbConnector;
import org.flexiblepower.connectors.ProcessConnector;
import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.Connection;
import org.flexiblepower.model.Connection.Endpoint;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.Process.Parameter;
import org.flexiblepower.model.Process.ProcessState;
import org.flexiblepower.orchestrator.pendingchange.PendingChange;
import org.flexiblepower.orchestrator.pendingchange.PendingChangeManager;
import org.mongodb.morphia.annotations.Entity;

import lombok.extern.slf4j.Slf4j;

/**
 * MoveProcess
 *
 * @author wilco
 * @version 0.1
 * @since Aug 14, 2017
 */
public class MoveProcess {

    @Entity("PendingChange")
    @Slf4j
    public static class SuspendConnection extends PendingChange {

        private Connection connection;
        private Endpoint endpoint;

        public SuspendConnection() {
            super();
        }

        public SuspendConnection(final ObjectId userId,
                final Connection connection,
                final Connection.Endpoint endpoint) {
            super(userId);
            this.connection = connection;
            this.endpoint = endpoint;
        }

        @Override
        public String description() {
            return "Suspending connection for Process " + this.endpoint.getProcessId() + " with interface "
                    + this.endpoint.getInterfaceId() + " to process "
                    + this.connection.getOtherEndpoint(this.endpoint).getProcessId() + " with connection "
                    + this.connection.getOtherEndpoint(this.endpoint).getInterfaceId();
        }

        @Override
        public Result execute() {
            try {
                if (ProcessConnector.getInstance().suspendConnectionEndpoint(this.connection, this.endpoint)) {
                    SuspendConnection.log.info("Successfully signaled process " + this.endpoint.getProcessId()
                            + " to suspend connection " + this.connection.getId());
                    return Result.SUCCESS;
                } else {
                    SuspendConnection.log.debug("Failed to signal process " + this.endpoint.getProcessId()
                            + " to suspend connection " + this.connection.getId());
                    return Result.FAILED_TEMPORARY;
                }
            } catch (final ProcessNotFoundException e) {
                SuspendConnection.log.error("Process {} not present in DB, fail permanently",
                        this.endpoint.getProcessId());
                return Result.FAILED_PERMANENTLY;
            }
        }
    }

    @Entity("PendingChange")
    @Slf4j
    public static class SuspendProcess extends PendingChange {

        private Process process;
        private ObjectId nodePoolId;
        private ObjectId privateNodeId;

        public SuspendProcess() {
            super();
        }

        public SuspendProcess(final Process process, final ObjectId nodePoolId, final ObjectId privateNodeId) {
            super(process.getUserId());
            this.process = process;
            this.nodePoolId = nodePoolId;
            this.privateNodeId = privateNodeId;
            if ((nodePoolId != null) && (privateNodeId != null)) {
                throw new IllegalArgumentException("Either nodePoolId or privateNodeId should be null");
            }

        }

        @Override
        public String description() {
            return "Suspend process " + this.process.getId();
        }

        @Override
        public long delayMs() {
            // Give it some time to suspend connections
            return 5000;
        }

        @Override
        public long retryIntervalMs() {
            return 1000;
        }

        @Override
        public Result execute() {
            byte[] suspendProcess;

            try {
                suspendProcess = ProcessConnector.getInstance().suspendProcess(this.process.getId());
            } catch (final ProcessNotFoundException e) {
                SuspendProcess.log.error("No such process {}, failed permanently", this.process.getId());
                return Result.FAILED_PERMANENTLY;
            }

            if (suspendProcess == null) {
                // that means it was not successful

                if (this.getCount() > 3) {
                    // Ok, this guy doesn't really want to suspend. Too bad, but we have to move on.
                    PendingChangeManager.getInstance().submit(
                            new RemoveDockerService(this.process, this.nodePoolId, this.privateNodeId, suspendProcess));
                    return Result.FAILED_PERMANENTLY;
                }

                return Result.FAILED_TEMPORARY;
            } else {
                // success!
                SuspendProcess.log.info("Suspended process " + this.process.getId() + " to move it");

                // Update the database
                this.process.setState(ProcessState.SUSPENDED);
                MongoDbConnector.getInstance().save(this.process);

                // start next
                PendingChangeManager.getInstance().submit(
                        new RemoveDockerService(this.process, this.nodePoolId, this.privateNodeId, suspendProcess));

                return Result.SUCCESS;
            }
        }

    }

    @Entity("PendingChange")
    @Slf4j
    public static class RemoveDockerService extends PendingChange {

        private Process process;
        private ObjectId nodePoolId;
        private ObjectId privateNodeId;
        private byte[] suspendState;

        public RemoveDockerService() {
            super();
        }

        public RemoveDockerService(final Process process,
                final ObjectId nodePoolId,
                final ObjectId privateNodeId,
                final byte[] suspendProcess) {
            super(process.getUserId());
            this.process = process;
            this.nodePoolId = nodePoolId;
            this.privateNodeId = privateNodeId;
            this.suspendState = suspendProcess;
            if ((nodePoolId != null) && (privateNodeId != null)) {
                throw new IllegalArgumentException("Either nodePoolId or privateNodeId should be null");
            }
        }

        @Override
        public String description() {
            return "Remove Docker Service for process " + this.process.getId() + " while moving process";
        }

        @Override
        public Result execute() {
            ProcessConnector.getInstance().disconnect(this.process.getId());

            try {
                if (DockerConnector.getInstance().removeProcess(this.process)) {
                    RemoveDockerService.log.info(
                            "Removed Docker Service for process " + this.process.getId() + " while moving the process");
                    // Start next step
                    PendingChangeManager.getInstance()
                            .submit(new CreateDockerService(this.process,
                                    this.nodePoolId,
                                    this.privateNodeId,
                                    this.suspendState));

                    return Result.SUCCESS;
                } else {
                    RemoveDockerService.log.info("Failed to remove Docker Service for process " + this.process.getId()
                            + " while moving the process");
                    return Result.FAILED_TEMPORARY;
                }
            } catch (final ProcessNotFoundException e) {
                RemoveDockerService.log.info("No such process {}", this.process.getId());
                return Result.FAILED_PERMANENTLY;
            }
        }
    }

    @Entity("PendingChange")
    @Slf4j
    public static class CreateDockerService extends PendingChange {

        private Process process;
        private ObjectId nodePoolId;
        private ObjectId privateNodeId;
        private byte[] suspendState;

        public CreateDockerService() {
            super();
        }

        @Override
        public long delayMs() {
            return 5000;
        }

        public CreateDockerService(final Process process,
                final ObjectId nodePoolId,
                final ObjectId privateNodeId,
                final byte[] suspendState) {
            super(process.getUserId());
            this.process = process;
            this.nodePoolId = nodePoolId;
            this.privateNodeId = privateNodeId;
            this.suspendState = suspendState;
            if ((nodePoolId != null) && (privateNodeId != null)) {
                throw new IllegalArgumentException("euther nodePoolId or privateNodeId should be null");
            }
        }

        @Override
        public String description() {
            return "Create Docker Service for process " + this.process.getId() + " while moving process";
        }

        @Override
        public Result execute() {
            try {
                this.process.setNodePoolId(this.nodePoolId);
                this.process.setPrivateNodeId(this.privateNodeId);

                final String newDockerId = DockerConnector.getInstance().newProcess(this.process);
                if (newDockerId != null) {
                    CreateDockerService.log.info(
                            "Created Docker Service for process " + this.process.getId() + " while moving the process");

                    // save in database
                    this.process.setDockerId(newDockerId);
                    MongoDbConnector.getInstance().save(this.process);

                    // Start next step
                    PendingChangeManager.getInstance().submit(new ResumeProcess(this.process, this.suspendState));

                    return Result.SUCCESS;
                } else {
                    CreateDockerService.log.info("Failed to create Docker Service for process " + this.process.getId()
                            + " while moving the process");
                    return Result.FAILED_TEMPORARY;
                }
            } catch (final ServiceNotFoundException e) {
                SuspendConnection.log.error("Process {} not present in DB, fail permanently", this.process.getId());
                return Result.FAILED_PERMANENTLY;
            }
        }
    }

    @Entity("PendingChange")
    @Slf4j
    public static class ResumeProcess extends PendingChange {

        private Process process;
        private List<Parameter> configuration;
        private byte[] suspendState;

        public ResumeProcess() {
            super();
        }

        public ResumeProcess(final Process process, final byte[] suspendState) {
            super(process.getUserId());
            this.process = process;
            this.suspendState = suspendState;
        }

        @Override
        public String description() {
            return "Resume Process " + this.process.getId() + " after moving process";
        }

        @Override
        public Result execute() {
            this.process.setConfiguration(this.configuration);

            try {
                if (ProcessConnector.getInstance().resume(this.process.getId(), this.suspendState)) {
                    ResumeProcess.log.info("Resumed process " + this.process.getId() + " after moving the process");

                    // resume connections
                    final PendingChangeManager pcm = PendingChangeManager.getInstance();
                    for (final Connection c : ConnectionManager.getInstance().getConnectionsForProcess(this.process)) {
                        pcm.submit(new MoveProcess.ResumeConnection(this.process.getUserId(), c, c.getEndpoint1()));
                        pcm.submit(new MoveProcess.ResumeConnection(this.process.getUserId(), c, c.getEndpoint2()));
                    }

                    return Result.SUCCESS;
                } else {
                    ResumeProcess.log
                            .info("Failed to resume process " + this.process.getId() + " after moving the process");
                    return Result.FAILED_TEMPORARY;
                }
            } catch (final ProcessNotFoundException e) {
                ResumeProcess.log.error("No such process {}, failed permanently", this.process.getId());
                return Result.FAILED_PERMANENTLY;
            }
        }

    }

    @Entity("PendingChange")
    @Slf4j
    public static class ResumeConnection extends PendingChange {

        private Connection connection;
        private Endpoint endpoint;

        public ResumeConnection() {
            super();
        }

        public ResumeConnection(final ObjectId userId,
                final Connection connection,
                final Connection.Endpoint endpoint) {
            super(userId);
            this.connection = connection;
            this.endpoint = endpoint;
        }

        @Override
        public String description() {
            return "Resuming connection for Process " + this.endpoint.getProcessId() + " with interface "
                    + this.endpoint.getInterfaceId() + " to process "
                    + this.connection.getOtherEndpoint(this.endpoint).getProcessId() + " with connection "
                    + this.connection.getOtherEndpoint(this.endpoint).getInterfaceId();
        }

        @Override
        public Result execute() {
            try {
                if (ProcessConnector.getInstance().resumeConnectionEndpoint(this.connection, this.endpoint)) {
                    ResumeConnection.log.info("Successfully signaled process " + this.endpoint.getProcessId()
                            + " to resume connection " + this.connection.getId());
                    return Result.SUCCESS;
                } else {
                    ResumeConnection.log.debug("Failed to signal process " + this.endpoint.getProcessId()
                            + " to resume connection " + this.connection.getId());
                    return Result.FAILED_TEMPORARY;
                }
            } catch (final ProcessNotFoundException e) {
                ResumeConnection.log.info("Failed to resume connection for unkown process {}, failed permanently",
                        this.endpoint.getProcessId());
                return Result.FAILED_PERMANENTLY;
            } catch (final ServiceNotFoundException e) {
                ResumeConnection.log.info("Could not find service for process {}, failed permanently",
                        this.endpoint.getProcessId());
                return Result.FAILED_PERMANENTLY;
            }
        }
    }

}
