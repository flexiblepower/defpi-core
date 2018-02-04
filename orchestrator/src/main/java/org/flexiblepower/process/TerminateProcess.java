/**
 * File TerminateProcess.java
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
public class TerminateProcess {

    @Slf4j
    @Entity("PendingChange")
    public static class SendTerminateSignal extends PendingChange {

        private Process process;

        @Override
        public long delayMs() {
            return 2000;
        }

        // Default constructor for morphia
        @SuppressWarnings("unused")
        private SendTerminateSignal() {
            super();
        }

        public SendTerminateSignal(final Process process) {
            super(process.getUserId());
            this.resources = Collections.unmodifiableList(Arrays.asList(process.getId()));
            this.process = process;
        }

        @Override
        public String description() {
            return "Send terminate signal to process " + this.process.getId();
        }

        @Override
        public int maxRetryCount() {
            // This one does not get that many changes
            return 3;
        }

        @Override
        public long retryIntervalMs() {
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

    @Slf4j
    @Entity("PendingChange")
    public static class RemoveDockerService extends PendingChange {

        private static final int NUM_TRIES_TO_TERMINATE_CONNECTIONS = 10;

        private Process process;

        // Default constructor for morphia
        @SuppressWarnings("unused")
        private RemoveDockerService() {
            super();
        }

        public RemoveDockerService(final Process process) {
            super(process.getUserId());
            this.resources = Collections.unmodifiableList(Arrays.asList(process.getId()));
            this.process = process;
        }

        @Override
        public String description() {
            return "Removing Docker Service for process " + this.process.getId();
        }

        @Override
        public long delayMs() {
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

            boolean removeDbRecord;
            try {
                removeDbRecord = DockerConnector.getInstance().removeProcess(this.process);
                if (removeDbRecord) {
                    RemoveDockerService.log
                            .debug("Removing Docker service for process " + this.process.getId() + " was successful");
                }
                // Delete record from MongoDB
            } catch (final ProcessNotFoundException e) {
                RemoveDockerService.log.warn("Trying to remove Docker Service, but is already gone...");
                removeDbRecord = true;
            }
            if (removeDbRecord) {
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
