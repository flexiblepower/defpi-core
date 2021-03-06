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
package org.flexiblepower.process;

import java.util.Collections;

import org.flexiblepower.connectors.DockerConnector;
import org.flexiblepower.connectors.MongoDbConnector;
import org.flexiblepower.connectors.ProcessConnector;
import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.model.Process;
import org.flexiblepower.orchestrator.pendingchange.PendingChange;
import org.mongodb.morphia.annotations.Entity;

import lombok.extern.slf4j.Slf4j;

/**
 * TerminateProcess
 *
 * @version 0.1
 * @since Aug 14, 2017
 */
class TerminateProcess {

    /**
     * SendTerminateSignal
     *
     * @version 0.1
     * @since Aug 14, 2017
     */
    @Slf4j
    @Entity("PendingChange")
    static class SendTerminateSignal extends PendingChange {

        private Process process;

        @Override
        protected long delayMs() {
            return 2000;
        }

        // Default constructor for morphia
        @SuppressWarnings("unused")
        private SendTerminateSignal() {
            super();
        }

        /**
         * Create a pending change to terminate a process by sending it a TERMINATE signal.
         *
         * @param process The process to terminate
         */
        SendTerminateSignal(final Process process) {
            super(process.getUserId());
            this.resources = Collections.singletonList(process.getId());
            this.process = process;
        }

        @Override
        public String description() {
            return "Send terminate signal to process " + this.process.getId();
        }

        @Override
        protected int maxRetryCount() {
            // This one does not get that many changes
            return 3;
        }

        @Override
        protected long retryIntervalMs() {
            // This one does not get that many changes
            return 1000;
        }

        @Override
        public Result execute() {
            try {
                if (ProcessConnector.getInstance().terminate(this.process.getId())) {
                    SendTerminateSignal.log
                            .debug("Sending terminate signal to process " + this.process.getId() + " was successful");
                    return Result.SUCCESS;
                } else {
                    SendTerminateSignal.log
                            .debug("Sending terminate signal to process " + this.process.getId() + " failed");
                    return Result.FAILED_TEMPORARY;
                }
            } catch (final ProcessNotFoundException e) {
                SendTerminateSignal.log.error("No such process {}, failed permanently", this.process.getId());
                return Result.FAILED_PERMANENTLY;
            }
        }

    }

    /**
     * RemoveDockerService
     *
     * @version 0.1
     * @since Aug 14, 2017
     */
    @Slf4j
    @Entity("PendingChange")
    static class RemoveDockerService extends PendingChange {

        private static final int NUM_TRIES_TO_TERMINATE_CONNECTIONS = 10;

        private Process process;

        // Default constructor for morphia
        @SuppressWarnings("unused")
        private RemoveDockerService() {
            super();
        }

        /**
         * Create a pending change to terminate a process by removing the docker service
         *
         * @param process The process to terminate
         */
        RemoveDockerService(final Process process) {
            super(process.getUserId());
            this.resources = Collections.singletonList(process.getId());
            this.process = process;
        }

        @Override
        public String description() {
            return "Removing Docker Service for process " + this.process.getId();
        }

        @Override
        protected long delayMs() {
            // Give it some time to handle the terminate signal first
            return 5000;
        }

        @Override
        public Result execute() {
            if (!ConnectionManager.getInstance().getConnectionsForProcess(this.process).isEmpty()
                    && (this.getCount() < RemoveDockerService.NUM_TRIES_TO_TERMINATE_CONNECTIONS)) {
                RemoveDockerService.log.debug("There still exist connections, delay removal of docker service");
                return Result.FAILED_TEMPORARY;
            }

            ProcessConnector.getInstance().disconnect(this.process.getId());

            if (DockerConnector.getInstance().removeProcess(this.process)) {
                // Delete record from MongoDB
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

}
