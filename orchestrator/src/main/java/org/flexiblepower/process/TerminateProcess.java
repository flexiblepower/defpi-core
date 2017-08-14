/**
 * File TerminateProcess.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.process;

import org.flexiblepower.connectors.DockerConnector;
import org.flexiblepower.connectors.MongoDbConnector;
import org.flexiblepower.connectors.ProcessConnector;
import org.flexiblepower.model.Process;
import org.flexiblepower.orchestrator.pendingchange.PendingChange;
import org.mongodb.morphia.annotations.Entity;

import lombok.extern.slf4j.Slf4j;

/**
 * TerminateProcess
 *
 * @author wilco
 * @version 0.1
 * @since Aug 14, 2017
 */
public class TerminateProcess {

    @Slf4j
    @Entity("PendingChange")
    public static class SendTerminateSignal extends PendingChange {

        private Process process;

        public SendTerminateSignal() {
            super();
        }

        public SendTerminateSignal(final Process process) {
            super(process.getUserId());
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
            if (ProcessConnector.getInstance().terminate(this.process.getId())) {
                SendTerminateSignal.log
                        .debug("Sending terminate signal to process " + this.process.getId() + " was successful");
                return Result.SUCCESS;
            } else {
                SendTerminateSignal.log
                        .debug("Sending terminate signal to process " + this.process.getId() + " failed");
                return Result.FAILED_TEMPORARY;
            }
        }

    }

    @Slf4j
    @Entity("PendingChange")
    public static class RemoveDockerService extends PendingChange {

        private Process process;

        public RemoveDockerService() {
            super();
        }

        public RemoveDockerService(final Process process) {
            super(process.getUserId());
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
            if (DockerConnector.getInstance().removeProcess(this.process)) {
                RemoveDockerService.log
                        .debug("Removing Docker service for process " + this.process.getId() + " was successful");
                // Delete record from MongoDB
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
