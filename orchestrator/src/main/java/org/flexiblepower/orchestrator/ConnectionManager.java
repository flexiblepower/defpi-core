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
import org.flexiblepower.model.Interface;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.Service;
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

    public synchronized static ConnectionManager getInstance() {
        if (ConnectionManager.instance == null) {
            ConnectionManager.instance = new ConnectionManager();
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
    public Connection createConnection(final Connection connection) throws ProcessNotFoundException {
        ConnectionManager.validateConnection(connection);

        this.db.save(connection);
        this.pc.addConnection(connection);
        return connection;
    }

    private static void validateConnection(final Connection c) {
        if ((c.getProcess1Id() == null) || (c.getProcess2Id() == null)) {
            throw new IllegalArgumentException("ProcessId cannot be null");
        }
        if ((c.getInterface1Id() == null) || c.getInterface1Id().isEmpty() || (c.getInterface2Id() == null)
                || c.getInterface2Id().isEmpty()) {
            throw new IllegalArgumentException("Interface identifier cannot be empty");
        }
        final Process process1 = ProcessManager.getInstance().getProcess(c.getProcess1Id());
        if (process1 == null) {
            throw new IllegalArgumentException("No process with id " + c.getProcess1Id() + " known");
        }
        final Process process2 = ProcessManager.getInstance().getProcess(c.getProcess2Id());
        if (process2 == null) {
            throw new IllegalArgumentException("No process with id " + c.getProcess2Id() + " known");
        }
        final Service service1 = ServiceManager.getInstance().getService(process1.getServiceId());
        if (service1 == null) {
            throw new IllegalArgumentException("Could not find service " + process1.getServiceId());
        }
        final Service service2 = ServiceManager.getInstance().getService(process2.getServiceId());
        if (service2 == null) {
            throw new IllegalArgumentException("Could not find service " + process2.getServiceId());
        }
        final Interface if1 = service1.getInterface(c.getInterface1Id());
        if (if1 == null) {
            throw new IllegalArgumentException("The Service of Process 1 with id " + c.getProcess1Id()
                    + " does not contain the interface " + c.getInterface1Id());
        }
        final Interface if2 = service1.getInterface(c.getInterface2Id());
        if (if2 == null) {
            throw new IllegalArgumentException("The Service of Process 2 with id " + c.getProcess2Id()
                    + " does not contain the interface " + c.getInterface2Id());
        }
        if (!if1.isCompatibleWith(if2)) {
            throw new IllegalArgumentException("Interface " + c.getInterface1Id() + " and interface "
                    + c.getInterface2Id() + " are not compatible with each other");
        }
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
