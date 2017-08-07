/**
 * File ChangeProcessConfiguration.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.process;

import java.util.List;

import org.bson.types.ObjectId;
import org.flexiblepower.connectors.MongoDbConnector;
import org.flexiblepower.connectors.ProcessConnector;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.Process.Parameter;
import org.flexiblepower.orchestrator.pendingchange.PendingChange;
import org.mongodb.morphia.annotations.Entity;

import lombok.extern.slf4j.Slf4j;

/**
 * ChangeProcessConfiguration
 *
 * @author wilco
 * @version 0.1
 * @since Aug 7, 2017
 */
@Entity("PendingChange")
@Slf4j
public class ChangeProcessConfiguration extends PendingChange {

    private ObjectId processId;
    private List<Parameter> newConfigurtaion;

    public ChangeProcessConfiguration() {
        super();
    }

    public ChangeProcessConfiguration(final ObjectId userId,
            final ObjectId processId,
            final List<Parameter> newConfiguration) {
        super(0, 5000, userId);
        this.processId = processId;
        this.newConfigurtaion = newConfiguration;
    }

    @Override
    public String description() {
        return "Update configuration of process ";
    }

    @Override
    public Result execute() {
        ChangeProcessConfiguration.log.debug("Attempting to update configuration of process " + this.processId);
        final boolean success = ProcessConnector.getInstance().updateConfiguration(this.processId,
                this.newConfigurtaion);

        if (success) {
            ChangeProcessConfiguration.log.debug(
                    "Changing configuration of process " + this.processId + " was succesful, writing change to db");
            final MongoDbConnector db = MongoDbConnector.getInstance();
            final Process process = db.get(Process.class, this.processId);
            process.setConfiguration(this.newConfigurtaion);
            db.save(process);
            return Result.SUCCESS;
        } else {
            ChangeProcessConfiguration.log.debug("Changing configuration of process " + this.processId + " failed");
            return Result.FAILED_TEMPORARY;
        }
    }

}
