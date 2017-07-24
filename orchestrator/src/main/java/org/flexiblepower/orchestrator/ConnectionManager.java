/**
 * File ConnectionManager.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator;

import java.util.List;

import org.flexiblepower.exceptions.InvalidObjectIdException;
import org.flexiblepower.model.Connection;

/**
 * ConnectionManager
 *
 * @author coenvl
 * @version 0.1
 * @since Jul 24, 2017
 */
public class ConnectionManager {

    private static ConnectionManager instance;

    private final MongoDbConnector db = MongoDbConnector.getInstance();

    private ConnectionManager() {
        // Private constructor
    }

    public static ConnectionManager getInstance() {
        synchronized (ConnectionManager.instance) {
            if (ConnectionManager.instance == null) {
                ConnectionManager.instance = new ConnectionManager();
            }
        }
        return ConnectionManager.instance;
    }

    /**
     * @return a list of all connections that are stored in the database
     */
    public List<Connection> getConnections() {
        return this.db.list(Connection.class);
    }

    /**
     * Returns the connection that is stored in the database with the provided id, or null if no such connection exists.
     *
     * @param connectionId
     * @return the connection that has the provided id, or null
     * @throws InvalidObjectIdException
     */
    public Connection getConnection(final String connectionId) throws InvalidObjectIdException {
        return this.db.get(Connection.class, connectionId);
    }

    /**
     * Insert the provided connection in the database.
     *
     * @param connection
     * @return the id of the newly inserted connection
     */
    public String insertConnection(final Connection connection) {
        return this.db.save(connection);
    }

    /**
     * Removes the connection that has the provided id from the database.
     *
     * @param connectionId
     * @throws InvalidObjectIdException
     */
    public void deleteConnection(final String connectionId) throws InvalidObjectIdException {
        this.db.delete(Connection.class, connectionId);
    }

    /**
     * @return a list of all connections that are connected to the process with the provided id
     */
    public List<Connection> getConnectionsForProcess(final String processId) {
        return this.db.getConnectionsForProcess(processId);
    }

    /**
     * Removes all connections that are connected to the process with the provided id from the database.
     *
     * @param processId
     */
    public void deleteConnectionsForProcess(final String processId) {
        for (final Connection connection : this.getConnectionsForProcess(processId)) {
            // DO stuff.
            this.db.delete(connection);
        }
    }

}
