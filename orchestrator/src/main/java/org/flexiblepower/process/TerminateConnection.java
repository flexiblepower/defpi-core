/**
 * File CreateConnection.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.process;

import org.bson.types.ObjectId;
import org.flexiblepower.connectors.ProcessConnector;
import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.model.Connection;
import org.flexiblepower.model.Connection.Endpoint;
import org.flexiblepower.orchestrator.pendingchange.PendingChange;
import org.mongodb.morphia.annotations.Entity;

import lombok.extern.slf4j.Slf4j;

/**
 * CreateConnection
 *
 * @author wilco
 * @version 0.1
 * @since Aug 14, 2017
 */
@Entity("PendingChange")
@Slf4j
public class TerminateConnection extends PendingChange {

    private Connection connection;
    private Endpoint endpoint;

    public TerminateConnection() {
        super();
    }

    public TerminateConnection(final ObjectId userId, final Connection connection, final Connection.Endpoint endpoint) {
        super(userId);
        this.connection = connection;
        this.endpoint = endpoint;
    }

    @Override
    public String description() {
        return "Removing connection for Process " + this.endpoint.getProcessId() + " with interface "
                + this.endpoint.getInterfaceId() + " to process "
                + this.connection.getOtherEndpoint(this.endpoint).getProcessId() + " with connection "
                + this.connection.getOtherEndpoint(this.endpoint).getInterfaceId();
    }

    @Override
    public Result execute() {
        try {
            if (ProcessConnector.getInstance().terminateConnectionEndpoint(this.connection, this.endpoint)) {
                TerminateConnection.log.info("Successfully signaled process " + this.endpoint.getProcessId()
                        + " to terminate connection " + this.connection.getId());
                return Result.SUCCESS;
            } else {
                TerminateConnection.log.debug("Failed to signal process " + this.endpoint.getProcessId()
                        + " to terminate connection " + this.connection.getId());
                return Result.FAILED_TEMPORARY;
            }
        } catch (final ProcessNotFoundException e) {
            TerminateConnection.log.info("Failed to terminate process {}, failed permanently",
                    this.endpoint.getProcessId());
            return Result.FAILED_PERMANENTLY;
        }
    }

}
