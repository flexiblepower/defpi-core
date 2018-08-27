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

import java.util.Arrays;
import java.util.Collections;

import org.bson.types.ObjectId;
import org.flexiblepower.connectors.ProcessConnector;
import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.model.Connection;
import org.flexiblepower.model.Connection.Endpoint;
import org.flexiblepower.orchestrator.pendingchange.PendingChange;
import org.mongodb.morphia.annotations.Entity;

import lombok.extern.slf4j.Slf4j;

/**
 * TerminateConnection
 *
 * @version 0.1
 * @since Aug 14, 2017
 */
@Entity("PendingChange")
@Slf4j
class TerminateConnection extends PendingChange {

    private Connection connection;
    private Endpoint endpoint;

    // Default constructor for morphia
    @SuppressWarnings("unused")
    private TerminateConnection() {
        super();
    }

    /**
     * Terminate a connection endpoint
     *
     * @param userId The user who owns the process to connect
     * @param connection The Connection to terminate
     * @param endpoint The endpoint to terminate
     */
    TerminateConnection(final ObjectId userId, final Connection connection, final Connection.Endpoint endpoint) {
        super(userId);
        this.resources = Collections.unmodifiableList(Arrays.asList(connection.getId(),
                endpoint.getProcessId(),
                this.getUserId(),
                connection.getOtherEndpoint(endpoint).getProcessId()));
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
            TerminateConnection.log.info("Failed to terminate connection {}, failed permanently",
                    this.endpoint.getProcessId());
            return Result.FAILED_PERMANENTLY;
        }
    }

}
