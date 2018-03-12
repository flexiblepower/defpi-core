/**
 * File MoveProcess.java
 *
 * Copyright 2017 FAN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flexiblepower.process;

import java.util.Arrays;
import java.util.Collections;
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
import org.flexiblepower.model.Process.ProcessParameter;
import org.flexiblepower.model.Process.ProcessState;
import org.flexiblepower.orchestrator.pendingchange.PendingChange;
import org.flexiblepower.orchestrator.pendingchange.PendingChangeManager;
import org.mongodb.morphia.annotations.Entity;

import lombok.extern.slf4j.Slf4j;

/**
 * MoveProcess
 *
 * @version 0.1
 * @since Aug 14, 2017
 */
public class MoveProcess {

    /**
     * SuspendConnection
     *
     * @version 0.1
     * @since Aug 14, 2017
     */
    @Slf4j
    @Entity("PendingChange")
    public static class SuspendConnection extends PendingChange {

        private Connection connection;
        private Endpoint endpoint;

        @SuppressWarnings("unused")
        private SuspendConnection() {
            // Default constructor for morphia
            super();
        }

        /**
         * Suspend connection endpoint
         *
         * @param userId The user who owns the process to connect
         * @param connection The Connection to suspend
         * @param endpoint The endpoint to suspend
         */
        public SuspendConnection(final ObjectId userId,
                final Connection connection,
                final Connection.Endpoint endpoint) {
            super(userId);
            this.resources = Collections.unmodifiableList(Arrays.asList(connection.getId(), endpoint.getProcessId()));
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

    /**
     * SuspendProcess
     *
     * @version 0.1
     * @since Aug 14, 2017
     */
    @Entity("PendingChange")
    @Slf4j
    public static class SuspendProcess extends PendingChange {

        private Process process;
        private ObjectId nodePoolId;
        private ObjectId privateNodeId;

        // Default constructor for morphia
        @SuppressWarnings("unused")
        private SuspendProcess() {
            super();
        }

        /**
         * Create a pending change to suspend a process with the end goal of moving it. To move it, either the
         * nodePoolId or the privateNodeId must be set. When both are set, the nodepool takes precedence.
         *
         * @param process The process to suspend
         * @param nodePoolId The ID of the nodepool to move the process to (maybe null)
         * @param privateNodeId The ID of the private node to move the process to (maybe null)
         */
        public SuspendProcess(final Process process, final ObjectId nodePoolId, final ObjectId privateNodeId) {
            super(process.getUserId());
            this.resources = Collections.unmodifiableList(Arrays.asList(process.getId()));
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
        protected long delayMs() {
            // Give it some time to suspend connections
            return 5000;
        }

        @Override
        protected long retryIntervalMs() {
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
                    PendingChangeManager.getInstance()
                            .submit(new RemoveDockerService(this.process, this.nodePoolId, this.privateNodeId));
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
                PendingChangeManager.getInstance()
                        .submit(new RemoveDockerService(this.process, this.nodePoolId, this.privateNodeId));

                return Result.SUCCESS;
            }
        }

    }

    /**
     * RemoveDockerService
     *
     * @version 0.1
     * @since Aug 14, 2017
     */
    @Entity("PendingChange")
    @Slf4j
    public static class RemoveDockerService extends PendingChange {

        private Process process;

        // Default constructor for morphia
        @SuppressWarnings("unused")
        private RemoveDockerService() {
            super();
        }

        /**
         * Create a pending change to remove a docker service with the end goal of moving a process. Either the
         * nodePoolId or the privateNodeId must be set. When both are set, the nodepool takes precedence.
         *
         * @param process The process to suspend
         * @param nodePoolId The ID of the nodepool to move the process to (maybe null)
         * @param privateNodeId The ID of the private node to move the process to (maybe null)
         */
        public RemoveDockerService(final Process process, final ObjectId nodePoolId, final ObjectId privateNodeId) {
            super(process.getUserId());
            this.resources = Collections.unmodifiableList(Arrays.asList(process.getId()));
            this.process = process;
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

            // Delete record from MongoDB
            if (DockerConnector.getInstance().removeProcess(this.process)) {
                RemoveDockerService.log
                        .debug("Removing Docker service for process " + this.process.getId() + " was successful");
                MongoDbConnector.getInstance().delete(this.process);
                return Result.SUCCESS;
            } else {
                RemoveDockerService.log
                        .debug("Removing Docker service for process " + this.process.getId() + " failed");
                return Result.FAILED_TEMPORARY;
            }
        }
    }

    /**
     * CreateDockerService
     *
     * @version 0.1
     * @since Aug 14, 2017
     */
    @Slf4j
    @Entity("PendingChange")
    public static class CreateDockerService extends PendingChange {

        private Process process;
        private ObjectId nodePoolId;
        private ObjectId privateNodeId;
        private byte[] suspendState;

        // Default constructor for morphia
        @SuppressWarnings("unused")
        private CreateDockerService() {
            super();
        }

        @Override
        protected long delayMs() {
            return 5000;
        }

        /**
         * Create a pending change to create a new docker service for a process when moving it. To move it, either the
         * nodePoolId or the privateNodeId must be set. When both are set, the nodepool takes precedence.
         *
         * @param process The process to create a new service for
         * @param nodePoolId The ID of the nodepool to move the process to (maybe null)
         * @param privateNodeId The ID of the private node to move the process to (maybe null)
         * @param suspendState The byte array that represents the state that the process must be resumed from
         */
        public CreateDockerService(final Process process,
                final ObjectId nodePoolId,
                final ObjectId privateNodeId,
                final byte[] suspendState) {
            super(process.getUserId());
            // Add the userId because making the network, and choosing the running node
            this.resources = Collections.unmodifiableList(Arrays.asList(process.getId(), this.getUserId()));
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
                CreateDockerService.log.error("Process {} not present in DB, fail permanently", this.process.getId());
                return Result.FAILED_PERMANENTLY;
            }
        }
    }

    /**
     * ResumeProcess
     *
     * @version 0.1
     * @since Aug 14, 2017
     */
    @Entity("PendingChange")
    @Slf4j
    public static class ResumeProcess extends PendingChange {

        private Process process;
        private List<ProcessParameter> configuration;
        private byte[] suspendState;

        // Default constructor for morphia
        @SuppressWarnings("unused")
        private ResumeProcess() {
            super();
        }

        /**
         * Create a pending change to resume a moved process.
         *
         * @param process The process to resume
         * @param suspendState The byte array that represents the state that the process must be resumed from
         */
        public ResumeProcess(final Process process, final byte[] suspendState) {
            super(process.getUserId());
            this.resources = Collections.unmodifiableList(Arrays.asList(process.getId()));
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

    /**
     * ResumeConnection
     *
     * @version 0.1
     * @since Aug 14, 2017
     */
    @Entity("PendingChange")
    @Slf4j
    public static class ResumeConnection extends PendingChange {

        private Connection connection;
        private Endpoint endpoint;

        // Default constructor for morphia
        @SuppressWarnings("unused")
        private ResumeConnection() {
            super();
        }

        /**
         * Resume a suspended connection endpoint
         *
         * @param userId The user who owns the process to connect
         * @param connection The Connection to resume
         * @param endpoint The endpoint to resume
         */
        public ResumeConnection(final ObjectId userId,
                final Connection connection,
                final Connection.Endpoint endpoint) {
            super(userId);
            this.resources = Collections.unmodifiableList(Arrays.asList(connection.getId(), endpoint.getProcessId()));
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
            }
        }
    }

}
