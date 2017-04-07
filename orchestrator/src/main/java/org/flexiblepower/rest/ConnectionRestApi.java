package org.flexiblepower.rest;

import java.util.Collection;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;

import org.flexiblepower.api.ConnectionApi;
import org.flexiblepower.exceptions.InvalidObjectIdException;
import org.flexiblepower.model.Connection;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConnectionRestApi extends BaseApi implements ConnectionApi {

    protected ConnectionRestApi(@Context final HttpHeaders httpHeaders,
            @Context final SecurityContext securityContext) {
        super(httpHeaders, securityContext);
    }

    @Override
    public Collection<Connection> listConnections() {
        return this.db.getConnections();
    }

    @Override
    public String newConnection(final Connection connection) {
        ConnectionRestApi.log.info("newConnection(): " + connection);
        return this.db.insertConnection(connection);
    }

    @Override
    public void deleteConnection(final String id) throws InvalidObjectIdException {
        ConnectionRestApi.log.info("deleteConnection(): " + id);
        this.db.deleteConnection(id);
    }
}
