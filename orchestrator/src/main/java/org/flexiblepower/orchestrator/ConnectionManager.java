/**
 * File ConnectionManager.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.flexiblepower.exceptions.InvalidObjectIdException;
import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.Connection;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.User;

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
    private final ProcessConnector pc = ProcessConnector.getInstance();

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
    public Connection getConnection(final ObjectId connectionId) throws InvalidObjectIdException {
        return this.db.get(Connection.class, connectionId);
    }

    /**
     * Insert the provided connection in the database.
     *
     * @param connection
     * @return the id of the newly inserted connection
     * @throws ServiceNotFoundException
     * @throws ProcessNotFoundException
     */
    public ObjectId insertConnection(final Connection connection) throws ProcessNotFoundException {
        this.pc.addConnection(connection);
        return this.db.save(connection);
    }

    /**
     * Removes the connection that has the provided id from the database.
     *
     * @param connection
     * @throws InvalidObjectIdException
     */
    public void deleteConnection(final Connection connection) {
        this.pc.removeConnection(connection);
        this.db.delete(connection);
    }

    /**
     * @return a list of all connections that are connected to the process with the provided id
     */
    public List<Connection> getConnectionsForProcess(final Process process) {
        return this.db.getConnectionsForProcess(process);
    }

    /**
     * @param sessionUser
     * @return
     */
    public List<Connection> getConnectionsForUser(final User user) {
        final List<Process> processes = ProcessManager.getInstance().listProcesses(user);
        final List<Connection> ret = new ArrayList<>();

        for (final Process p : processes) {
            ret.addAll(this.getConnectionsForProcess(p));
        }
        return ret;
    }

    /**
     * Removes all connections that are connected to the process with the provided id from the database.
     *
     * @param process
     */
    public void deleteConnectionsForProcess(final Process process) {
        for (final Connection connection : this.getConnectionsForProcess(process)) {
            this.deleteConnection(connection);
        }
    }
}
