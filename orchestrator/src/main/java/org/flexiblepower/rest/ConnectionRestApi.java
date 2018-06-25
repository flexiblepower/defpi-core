/**
 * File ConnectionRestApi.java
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

package org.flexiblepower.rest;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;

import org.bson.types.ObjectId;
import org.flexiblepower.api.ConnectionApi;
import org.flexiblepower.connectors.MongoDbConnector;
import org.flexiblepower.exceptions.ApiException;
import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.ConnectionException;
import org.flexiblepower.exceptions.InvalidObjectIdException;
import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.Connection;
import org.flexiblepower.model.Process;
import org.flexiblepower.process.ConnectionManager;
import org.flexiblepower.process.ProcessManager;
import org.json.JSONObject;

import lombok.extern.slf4j.Slf4j;

/**
 * ConnectionRestApi
 *
 * @version 0.1
 * @since Mar 30, 2017
 */
@Slf4j
public class ConnectionRestApi extends BaseApi implements ConnectionApi {

    /**
     * Create the REST API with the headers from the HTTP request (will be injected by the HTTP server)
     *
     * @param httpHeaders The headers from the HTTP request for authorization
     */
    protected ConnectionRestApi(@Context final HttpHeaders httpHeaders) {
        super(httpHeaders);
    }

    @Override
    public List<Connection> listConnections(final int page,
            final int perPage,
            final String sortDir,
            final String sortField,
            final String filters) throws AuthorizationException {
        if ((page < 0) || (perPage < 0)) {
            this.addTotalCount(0);
            return Collections.emptyList();
        }

        List<Connection> connections;
        if (this.sessionUser == null) {
            throw new AuthorizationException();
        } else if (this.sessionUser.isAdmin()) {
            connections = ConnectionManager.getInstance().getConnections();
        } else {
            connections = ConnectionManager.getInstance().getConnectionsForUser(this.sessionUser);
        }

        if (filters != null) {
            final JSONObject f = new JSONObject(filters);
            if (f.has("userId")) {
                final Iterator<Connection> it = connections.iterator();
                while (it.hasNext()) {
                    final Connection c = it.next();
                    try {
                        final Process p1 = ProcessManager.getInstance().getProcess(c.getEndpoint1().getProcessId());
                        final Process p2 = ProcessManager.getInstance().getProcess(c.getEndpoint2().getProcessId());
                        if (!(p1.getUserId().toString().equals(f.getString("userId"))
                                || p2.getUserId().toString().equals(f.getString("userId")))) {
                            it.remove();
                        }
                    } catch (final ProcessNotFoundException e) {
                        // ignore
                    }
                }
            }
            if (f.has("processId")) {
                final Iterator<Connection> it = connections.iterator();
                while (it.hasNext()) {
                    final Connection c = it.next();
                    if (!(c.getEndpoint1().getProcessId().toString().equals(f.getString("processId"))
                            || c.getEndpoint2().getProcessId().toString().equals(f.getString("processId")))) {
                        it.remove();
                    }
                }
            }
        }

        // Now do the sorting
        final Comparator<Connection> comparator;
        switch (sortField) {
        case "endpoint1.interfaceId":
            comparator = (a, b) -> a.getEndpoint1().getInterfaceId().compareTo(b.getEndpoint1().getInterfaceId());
            break;
        case "endpoint2.interfaceId":
            comparator = (a, b) -> a.getEndpoint2().getInterfaceId().compareTo(b.getEndpoint2().getInterfaceId());
            break;
        case "endpoint1.processId":
            comparator = (a, b) -> a.getEndpoint1().getProcessId().compareTo(b.getEndpoint1().getProcessId());
            break;
        case "endpoint2.processId":
            comparator = (a, b) -> a.getEndpoint2().getProcessId().compareTo(b.getEndpoint2().getProcessId());
            break;
        case "id":
        default:
            comparator = (a, b) -> a.getId().toString().compareTo(b.getId().toString());
            break;
        }
        Collections.sort(connections, comparator);

        // Order the sorting if necessary
        if (sortDir.equals("DESC")) {
            Collections.reverse(connections);
        }

        // And finally pagination
        this.addTotalCount(connections.size());
        if ((page == 0) || (perPage == 0)) {
            return connections;
        }
        return connections.subList((page - 1) * perPage, Math.min(connections.size(), page * perPage));
    }

    @Override
    public Connection newConnection(final Connection connection) throws AuthorizationException,
            ProcessNotFoundException,
            ServiceNotFoundException,
            ConnectionException {
        final Process p1 = ProcessManager.getInstance().getProcess(connection.getEndpoint1().getProcessId());
        // if (p1 == null) {
        // throw new ProcessNotFoundException(connection.getEndpoint1().getProcessId());
        // }
        this.assertUserIsAdminOrEquals(p1.getUserId());

        final Process p2 = ProcessManager.getInstance().getProcess(connection.getEndpoint2().getProcessId());
        // if (p2 == null) {
        // throw new ProcessNotFoundException(connection.getEndpoint2().getProcessId());
        // }
        this.assertUserIsAdminOrEquals(p2.getUserId());

        ConnectionRestApi.log.info("Inserting new Connection {}", connection);
        try {
            ConnectionManager.getInstance().createConnection(connection);
        } catch (final IllegalArgumentException e) {
            throw new ApiException(400, e.getMessage());
        }
        return connection;
    }

    @Override
    public Connection getConnection(final String id) throws AuthorizationException,
            ProcessNotFoundException,
            InvalidObjectIdException {
        final ObjectId oid = MongoDbConnector.stringToObjectId(id);
        final Connection connection = ConnectionManager.getInstance().getConnection(oid);

        if (connection == null) {
            throw new ApiException(Status.NOT_FOUND, "Could not find connection with id " + id);
        }

        final Process p1 = ProcessManager.getInstance().getProcess(connection.getEndpoint1().getProcessId());
        // if (p1 == null) {
        // throw new ProcessNotFoundException(connection.getEndpoint1().getProcessId());
        // }
        this.assertUserIsAdminOrEquals(p1.getUserId());

        final Process p2 = ProcessManager.getInstance().getProcess(connection.getEndpoint2().getProcessId());
        // if (p2 == null) {
        // throw new ProcessNotFoundException(connection.getEndpoint2().getProcessId());
        // }
        this.assertUserIsAdminOrEquals(p2.getUserId());

        return connection;
    }

    @Override
    public void deleteConnection(final String id) throws InvalidObjectIdException,
            ProcessNotFoundException,
            AuthorizationException {
        final Connection connection = this.getConnection(id);

        final Process p1 = ProcessManager.getInstance().getProcess(connection.getEndpoint1().getProcessId());
        // if (p1 == null) {
        // throw new ProcessNotFoundException(connection.getEndpoint1().getProcessId());
        // }
        this.assertUserIsAdminOrEquals(p1.getUserId());

        final Process p2 = ProcessManager.getInstance().getProcess(connection.getEndpoint2().getProcessId());
        if (p2 == null) {
            throw new ProcessNotFoundException(connection.getEndpoint2().getProcessId());
        }
        this.assertUserIsAdminOrEquals(p2.getUserId());

        ConnectionRestApi.log.info("Removing connection {}", id);
        ConnectionManager.getInstance().terminateConnection(connection, p1.getUserId());
    }
}
