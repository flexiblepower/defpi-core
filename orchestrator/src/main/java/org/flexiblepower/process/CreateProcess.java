/**
 * File CreateProcess.java
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
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.Process.ProcessState;
import org.flexiblepower.orchestrator.pendingchange.PendingChange;
import org.mongodb.morphia.annotations.Entity;

import lombok.extern.slf4j.Slf4j;

/**
 * CreateProcess
 *
 * @version 0.1
 * @since Aug 9, 2017
 */
public class CreateProcess {

    /**
     * CreateDockerService
     *
     * @version 0.1
     * @since Aug 9, 2017
     */
    @Slf4j
    @Entity("PendingChange")
    public static class CreateDockerService extends PendingChange {

        private Process process;

        @SuppressWarnings("unused")
        private CreateDockerService() {
            // Default constructor for morphia
        }

        /**
         * Create a pending change to create the docker service of a process
         *
         * @param process The process to create
         */
        public CreateDockerService(final Process process) {
            super(process.getUserId());
            this.resources = Collections.unmodifiableList(Arrays.asList(process.getId()));
            this.process = process;
        }

        @Override
        public String description() {
            return "Create Docker Service for process " + this.process.getId();
        }

        @Override
        public Result execute() {
            // Create docker service
            CreateDockerService.log.info("Starting process " + this.process.getId());
            try {
                final String dockerId = DockerConnector.getInstance().newProcess(this.process);
                if (dockerId == null) {
                    return Result.FAILED_TEMPORARY;
                }

                // Update database
                this.process.setState(ProcessState.INITIALIZING);
                this.process.setDockerId(dockerId);
                MongoDbConnector.getInstance().save(this.process);

                // Start next PendingChange
                // NO! let the service query the orchestrator for a trigger. This way it will also work after it crashed
                // PendingChangeManager.getInstance().submit(new SendConfiguration(this.process));

                // Report
                return Result.SUCCESS;
            } catch (final ServiceNotFoundException e) {
                CreateDockerService.log.error("Unable to find service for process, fail permanently");
                return Result.FAILED_PERMANENTLY;
            }
        }

    }

    /**
     * SendConfiguration
     *
     * @version 0.1
     * @since Aug 9, 2017
     */
    @Slf4j
    @Entity("PendingChange")
    public static class SendConfiguration extends PendingChange {

        private Process process;

        // Default constructor for morphia
        @SuppressWarnings("unused")
        private SendConfiguration() {
        }

        /**
         * Create a pending change to send the configuration of a process
         *
         * @param process The process to configure
         */
        public SendConfiguration(final Process process) {
            super(process.getUserId());
            this.resources = Collections.unmodifiableList(Arrays.asList(process.getId()));
            this.process = process;
        }

        @Override
        public String description() {
            return "Initializing process " + this.process.getId();
        }

        @Override
        public Result execute() {
            SendConfiguration.log.info("Going to configure process " + this.process.getId());
            try {
                if (ProcessConnector.getInstance().initNewProcess(this.process.getId())) {
                    // Create autoconnect connections. This method will spawn PendingChanges of its own.
                    try {
                        ConnectionManager.getInstance().createAutoConnectConnections(this.process);
                    } catch (final ServiceNotFoundException e) {
                        SendConfiguration.log.error(
                                "Could not find service while trying to create autoconnect connection for new process. Failing permanently.");
                        return Result.FAILED_PERMANENTLY;
                    }
                    return Result.SUCCESS;
                } else {
                    return Result.FAILED_TEMPORARY;
                }
            } catch (final ProcessNotFoundException e) {
                SendConfiguration.log.error("Process {} not present in DB, fail permanently", this.process.getId());
                return Result.FAILED_PERMANENTLY;
            }
        }

    }

}
