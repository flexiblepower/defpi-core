/**
 * File CreateProcess.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.process;

import org.flexiblepower.connectors.DockerConnector;
import org.flexiblepower.connectors.MongoDbConnector;
import org.flexiblepower.connectors.ProcessConnector;
import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.Process.ProcessState;
import org.flexiblepower.orchestrator.pendingchange.PendingChange;
import org.flexiblepower.orchestrator.pendingchange.PendingChangeManager;
import org.mongodb.morphia.annotations.Entity;

import lombok.extern.slf4j.Slf4j;

/**
 * CreateProcess
 *
 * @author wilco
 * @version 0.1
 * @since Aug 9, 2017
 */
public class CreateProcess {

    @Slf4j
    @Entity("PendingChange")
    public static class CreateDockerService extends PendingChange {

        private Process process;

        public CreateDockerService() {
        }

        public CreateDockerService(final Process process) {
            super(process.getUserId());
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
                PendingChangeManager.getInstance().submit(new SendConfiguration(this.process));

                // Report
                return Result.SUCCESS;
            } catch (final ServiceNotFoundException e) {
                CreateDockerService.log.error("Unable to find service for process, fail permanently");
                return Result.FAILED_PERMANENTLY;
            }
        }

    }

    @Slf4j
    @Entity("PendingChange")
    public static class SendConfiguration extends PendingChange {

        private Process process;

        public SendConfiguration() {
        }

        public SendConfiguration(final Process process) {
            super(process.getUserId());
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
