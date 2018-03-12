/**
 * File ChangeProcessConfiguration.java
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

import org.flexiblepower.connectors.MongoDbConnector;
import org.flexiblepower.connectors.ProcessConnector;
import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.Process.ProcessParameter;
import org.flexiblepower.orchestrator.pendingchange.PendingChange;
import org.mongodb.morphia.annotations.Entity;

import lombok.extern.slf4j.Slf4j;

/**
 * ChangeProcessConfiguration
 *
 * @version 0.1
 * @since Aug 7, 2017
 */
@Entity("PendingChange")
@Slf4j
public class ChangeProcessConfiguration extends PendingChange {

    private Process process;
    private List<ProcessParameter> newConfiguration;

    // Default constructor for morphia
    @SuppressWarnings("unused")
    private ChangeProcessConfiguration() {
        super();
    }

    /**
     * Create a pending change to update the configuration of a process
     *
     * @param process The process to update
     * @param newConfiguration The new configuration as a list of parameters
     */
    public ChangeProcessConfiguration(final Process process, final List<ProcessParameter> newConfiguration) {
        super(process.getUserId());
        this.resources = Collections.unmodifiableList(Arrays.asList(process.getId()));
        this.process = process;
        this.newConfiguration = newConfiguration;
    }

    @Override
    public String description() {
        return "Update configuration of process " + this.process.getId();
    }

    @Override
    public Result execute() {
        ChangeProcessConfiguration.log.debug("Attempting to update configuration of process " + this.process.getId());
        boolean success;
        try {
            success = ProcessConnector.getInstance().updateConfiguration(this.process.getId(), this.newConfiguration);
        } catch (final ProcessNotFoundException e) {
            ChangeProcessConfiguration.log.error("No such process {}, failed permanently", this.process.getId());
            return Result.FAILED_PERMANENTLY;
        }

        if (success) {
            ChangeProcessConfiguration.log.debug("Changing configuration of process " + this.process.getId()
                    + " was succesful, writing change to db");
            final MongoDbConnector db = MongoDbConnector.getInstance();
            this.process.setConfiguration(this.newConfiguration);
            db.save(this.process);
            return Result.SUCCESS;
        } else {
            ChangeProcessConfiguration.log
                    .debug("Changing configuration of process " + this.process.getId() + " failed");
            return Result.FAILED_TEMPORARY;
        }
    }

}
