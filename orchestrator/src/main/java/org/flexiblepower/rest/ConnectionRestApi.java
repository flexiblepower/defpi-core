package org.flexiblepower.rest;

import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;

import org.bson.types.ObjectId;
import org.flexiblepower.api.ConnectionApi;
import org.flexiblepower.exceptions.ApiException;
import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.InvalidObjectIdException;
import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.model.Connection;
import org.flexiblepower.model.Process;
import org.flexiblepower.orchestrator.ConnectionManager;
import org.flexiblepower.orchestrator.MongoDbConnector;
import org.flexiblepower.orchestrator.ProcessManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConnectionRestApi extends BaseApi implements ConnectionApi {

    private final ConnectionManager connectionManager = ConnectionManager.getInstance();

    protected ConnectionRestApi(@Context final HttpHeaders httpHeaders) {
        super(httpHeaders);
    }

    @Override
    public List<Connection> listConnections() {
        if (this.sessionUser.isAdmin()) {
            return this.connectionManager.getConnections();
        } else {
            return this.connectionManager.getConnectionsForUser(this.sessionUser);
        }
    }

    @Override
    public Connection newConnection(final Connection connection) throws AuthorizationException,
            ProcessNotFoundException {
        final Process p1 = ProcessManager.getInstance().getProcess(connection.getProcess1Id());

        if (p1 == null) {
            throw new ProcessNotFoundException(connection.getProcess1Id().toString());
        }
        this.assertUserIsAdminOrEquals(p1.getUserId());

        final Process p2 = ProcessManager.getInstance().getProcess(connection.getProcess2Id());
        if (p2 == null) {
            throw new ProcessNotFoundException(connection.getProcess2Id().toString());
        }
        this.assertUserIsAdminOrEquals(p2.getUserId());

        ConnectionRestApi.log.info("Inserting new Connection {}", connection);
        this.connectionManager.insertConnection(connection);
        return connection;
    }

    @Override
    public Connection getConnection(final String id) throws AuthorizationException,
            ProcessNotFoundException,
            InvalidObjectIdException {
        final ObjectId oid = MongoDbConnector.stringToObjectId(id);
        final Connection connection = this.connectionManager.getConnection(oid);

        if (connection == null) {
            throw new ApiException(Status.NOT_FOUND, "Could not find connection with id " + id);
        }

        final Process p1 = ProcessManager.getInstance().getProcess(connection.getProcess1Id());
        if (p1 == null) {
            throw new ProcessNotFoundException(connection.getProcess1Id().toString());
        }
        this.assertUserIsAdminOrEquals(p1.getUserId());

        final Process p2 = ProcessManager.getInstance().getProcess(connection.getProcess2Id());
        if (p2 == null) {
            throw new ProcessNotFoundException(connection.getProcess2Id().toString());
        }
        this.assertUserIsAdminOrEquals(p2.getUserId());

        return connection;
    }

    @Override
    public void deleteConnection(final String id) throws InvalidObjectIdException,
            ProcessNotFoundException,
            AuthorizationException {
        final Connection connection = this.getConnection(id);

        ConnectionRestApi.log.info("Removing connection {}", id);
        this.connectionManager.deleteConnection(connection);
    }
}
