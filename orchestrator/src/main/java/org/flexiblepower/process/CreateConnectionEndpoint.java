/**
 * File CreateConnection.java
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

import org.bson.types.ObjectId;
import org.flexiblepower.connectors.ProcessConnector;
import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.Connection;
import org.flexiblepower.model.Connection.Endpoint;
import org.flexiblepower.orchestrator.pendingchange.PendingChange;
import org.mongodb.morphia.annotations.Entity;

import lombok.extern.slf4j.Slf4j;

/**
 * CreateConnection
 *
 * @version 0.1
 * @since Aug 14, 2017
 */
@Entity("PendingChange")
@Slf4j
public class CreateConnectionEndpoint extends PendingChange {

    private Connection connection;
    private Endpoint endpoint;

    public CreateConnectionEndpoint() {
        super();
    }

    public CreateConnectionEndpoint(final ObjectId userId, final Connection connection, final Connection.Endpoint endpoint) {
        super(userId);
        this.connection = connection;
        this.endpoint = endpoint;
    }

    @Override
    public String description() {
        return "Creating connection for Process " + this.endpoint.getProcessId() + " with interface "
                + this.endpoint.getInterfaceId() + " to process "
                + this.connection.getOtherEndpoint(this.endpoint).getProcessId() + " with connection "
                + this.connection.getOtherEndpoint(this.endpoint).getInterfaceId();
    }

    @Override
    public Result execute() {
        try {
            if (ProcessConnector.getInstance().createConnectionEndpoint(this.connection, this.endpoint)) {
                CreateConnectionEndpoint.log.info("Successfully signaled process " + this.endpoint.getProcessId()
                        + " to create connection " + this.connection.getId());
                return Result.SUCCESS;
            } else {
                CreateConnectionEndpoint.log.debug("Failed to signal process " + this.endpoint.getProcessId()
                        + " to create connection " + this.connection.getId());
                return Result.FAILED_TEMPORARY;
            }
        } catch (final ProcessNotFoundException | ServiceNotFoundException e) {
            CreateConnectionEndpoint.log.error("Could not signal process {} to create connection {} ({})",
                    this.endpoint.getProcessId(),
                    this.connection.getId(),
                    e.getMessage());
            return Result.FAILED_PERMANENTLY;
        }
    }

}
