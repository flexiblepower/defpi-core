package org.flexiblepower.rest;

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

@Slf4j
public class ConnectionRestApi extends BaseApi implements ConnectionApi {

    protected ConnectionRestApi(@Context final HttpHeaders httpHeaders) {
        super(httpHeaders);
    }

    @Override
    public List<Connection> listConnections(final String filters) throws AuthorizationException {
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
        return connections;

    }

    @Override
    public Connection newConnection(final Connection connection) throws AuthorizationException,
            ProcessNotFoundException,
            ServiceNotFoundException,
            ConnectionException {
        final Process p1 = ProcessManager.getInstance().getProcess(connection.getEndpoint1().getProcessId());

        if (p1 == null) {
            throw new ProcessNotFoundException(connection.getEndpoint1().getProcessId().toString());
        }
        this.assertUserIsAdminOrEquals(p1.getUserId());

        final Process p2 = ProcessManager.getInstance().getProcess(connection.getEndpoint2().getProcessId());
        if (p2 == null) {
            throw new ProcessNotFoundException(connection.getEndpoint2().getProcessId().toString());
        }
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
        if (p1 == null) {
            throw new ProcessNotFoundException(connection.getEndpoint1().getProcessId().toString());
        }
        this.assertUserIsAdminOrEquals(p1.getUserId());

        final Process p2 = ProcessManager.getInstance().getProcess(connection.getEndpoint2().getProcessId());
        if (p2 == null) {
            throw new ProcessNotFoundException(connection.getEndpoint2().getProcessId().toString());
        }
        this.assertUserIsAdminOrEquals(p2.getUserId());

        return connection;
    }

    @Override
    public void deleteConnection(final String id) throws InvalidObjectIdException,
            ProcessNotFoundException,
            AuthorizationException {
        final Connection connection = this.getConnection(id);

        final Process p1 = ProcessManager.getInstance().getProcess(connection.getEndpoint1().getProcessId());
        if (p1 == null) {
            throw new ProcessNotFoundException(connection.getEndpoint1().getProcessId().toString());
        }
        this.assertUserIsAdminOrEquals(p1.getUserId());

        final Process p2 = ProcessManager.getInstance().getProcess(connection.getEndpoint2().getProcessId());
        if (p2 == null) {
            throw new ProcessNotFoundException(connection.getEndpoint2().getProcessId().toString());
        }
        this.assertUserIsAdminOrEquals(p2.getUserId());

        ConnectionRestApi.log.info("Removing connection {}", id);
        ConnectionManager.getInstance().terminateConnection(connection);
    }
}
