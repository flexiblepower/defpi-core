package org.flexiblepower.rest;

import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

import org.flexiblepower.api.ConnectionApi;
import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.InvalidObjectIdException;
import org.flexiblepower.exceptions.NotFoundException;
import org.flexiblepower.model.Connection;
import org.flexiblepower.orchestrator.ConnectionManager;
import org.flexiblepower.orchestrator.MongoDbConnector;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConnectionRestApi extends BaseApi implements ConnectionApi {

    private final ConnectionManager connectionManager = ConnectionManager.getInstance();

    protected ConnectionRestApi(@Context final HttpHeaders httpHeaders) {
        super(httpHeaders);
    }

    @Override
    public List<Connection> listConnections() {
        return this.connectionManager.getConnections();
    }

    @Override
    public Connection newConnection(final Connection connection) throws AuthorizationException, NotFoundException {
        ConnectionRestApi.log.info("newConnection(): " + connection);
        return this.connectionManager.insertConnection(connection);
    }

    @Override
    public void deleteConnection(final String id) throws InvalidObjectIdException {
        ConnectionRestApi.log.info("deleteConnection(): " + id);
        this.connectionManager
                .deleteConnection(this.connectionManager.getConnection(MongoDbConnector.stringToObjectId(id)));
    }
}
