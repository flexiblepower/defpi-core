package org.flexiblepower.rest;

import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;

import org.flexiblepower.api.ConnectionApi;
import org.flexiblepower.exceptions.ConnectionException;
import org.flexiblepower.exceptions.InvalidObjectIdException;
import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.Connection;
import org.flexiblepower.orchestrator.ConnectionManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConnectionRestApi extends BaseApi implements ConnectionApi {

    private final ConnectionManager connections;

    protected ConnectionRestApi(@Context final HttpHeaders httpHeaders,
            @Context final SecurityContext securityContext) {
        super(httpHeaders, securityContext);
        this.connections = new ConnectionManager();
    }

    @Override
    public List<Connection> listConnections() {
        return this.db.getConnections();
    }

    @Override
    public String newConnection(final Connection connection) throws ProcessNotFoundException {
        ConnectionRestApi.log.info("newConnection(): " + connection);
        try {
            this.connections.addConnection(connection);
        } catch (final ConnectionException | ServiceNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return this.db.insertConnection(connection);
    }

    @Override
    public void deleteConnection(final String id) throws InvalidObjectIdException {
        ConnectionRestApi.log.info("deleteConnection(): " + id);
        this.db.deleteConnection(id);
    }
}
