/**
 * File ConnectionManager.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.bson.types.ObjectId;
import org.flexiblepower.connectors.MongoDbConnector;
import org.flexiblepower.connectors.ProcessConnector;
import org.flexiblepower.exceptions.InvalidObjectIdException;
import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.Connection;
import org.flexiblepower.model.Interface;
import org.flexiblepower.model.InterfaceVersion;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.Service;
import org.flexiblepower.model.User;
import org.flexiblepower.orchestrator.ServiceManager;
import org.flexiblepower.orchestrator.pendingchange.PendingChangeManager;

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
    public void createConnection(final Connection connection) throws ProcessNotFoundException {
        ConnectionManager.validateConnection(connection);

        ConnectionManager.setPorts(connection);

        ConnectionManager.setInterfaceNames(connection);

        final Process process1 = ProcessManager.getInstance().getProcess(connection.getEndpoint1().getProcessId());

        this.db.save(connection);

        PendingChangeManager.getInstance()
                .submit(new CreateConnection(process1.getUserId(), connection, connection.getEndpoint1()));
        PendingChangeManager.getInstance()
                .submit(new CreateConnection(process1.getUserId(), connection, connection.getEndpoint2()));
    }

    private static void setInterfaceNames(final Connection connection) {
        final Interface interface1 = ServiceManager.getInstance()
                .getInterfaceById(connection.getEndpoint1().getInterfaceId());
        final Interface interface2 = ServiceManager.getInstance()
                .getInterfaceById(connection.getEndpoint1().getInterfaceId());
        final ArrayList<InterfaceVersion> iface1 = new ArrayList<>(interface1.getInterfaceVersions());
        final ArrayList<InterfaceVersion> iface2 = new ArrayList<>(interface2.getInterfaceVersions());
        Collections.sort(iface1);
        Collections.sort(iface2);
        String best1 = null, best2 = null;
        for (final InterfaceVersion iv1 : iface1) {
            for (final InterfaceVersion iv2 : iface2) {
                if (iv1.isCompatibleWith(iv2)) {
                    best1 = iv1.getVersionName();
                    best2 = iv2.getVersionName();
                }
            }
        }
        connection.getEndpoint1().setInterfaceVersionName(best1);
        connection.getEndpoint2().setInterfaceVersionName(best2);
    }

    private static void setPorts(final Connection connection) {
        // TODO imlement better strategy
        connection.getEndpoint1().setListenPort(5000 + new Random().nextInt(5000));
        connection.getEndpoint2().setListenPort(5000 + new Random().nextInt(5000));
    }

    private static void validateConnection(final Connection c) {
        if ((c.getEndpoint1().getProcessId() == null) || (c.getEndpoint2().getProcessId() == null)) {
            throw new IllegalArgumentException("ProcessId cannot be null");
        }
        if ((c.getEndpoint1().getInterfaceId() == null) || c.getEndpoint1().getInterfaceId().isEmpty()
                || (c.getEndpoint2().getInterfaceId() == null) || c.getEndpoint2().getInterfaceId().isEmpty()) {
            throw new IllegalArgumentException("Interface identifier cannot be empty");
        }
        final Process process1 = ProcessManager.getInstance().getProcess(c.getEndpoint1().getProcessId());
        if (process1 == null) {
            throw new IllegalArgumentException("No process with id " + c.getEndpoint1().getProcessId() + " known");
        }
        final Process process2 = ProcessManager.getInstance().getProcess(c.getEndpoint2().getProcessId());
        if (process2 == null) {
            throw new IllegalArgumentException("No process with id " + c.getEndpoint2().getProcessId() + " known");
        }
        final Service service1 = ServiceManager.getInstance().getService(process1.getServiceId());
        if (service1 == null) {
            throw new IllegalArgumentException("Could not find service " + process1.getServiceId());
        }
        final Service service2 = ServiceManager.getInstance().getService(process2.getServiceId());
        if (service2 == null) {
            throw new IllegalArgumentException("Could not find service " + process2.getServiceId());
        }
        final Interface if1 = service1.getInterface(c.getEndpoint1().getInterfaceId());
        if (if1 == null) {
            throw new IllegalArgumentException("The Service of Process 1 with id " + c.getEndpoint1().getProcessId()
                    + " does not contain the interface " + c.getEndpoint1().getInterfaceId());
        }
        final Interface if2 = service1.getInterface(c.getEndpoint2().getInterfaceId());
        if (if2 == null) {
            throw new IllegalArgumentException("The Service of Process 2 with id " + c.getEndpoint2().getProcessId()
                    + " does not contain the interface " + c.getEndpoint2().getInterfaceId());
        }
        if (!if1.isCompatibleWith(if2)) {
            throw new IllegalArgumentException("Interface " + c.getEndpoint1().getInterfaceId() + " and interface "
                    + c.getEndpoint2().getInterfaceId() + " are not compatible with each other");
        }
    }

    /**
     * Removes the connection that has the provided id from the database.
     *
     * @param connection
     * @throws InvalidObjectIdException
     */
    public void terminateConnection(final Connection connection) {
        final Process process1 = ProcessManager.getInstance().getProcess(connection.getEndpoint1().getProcessId());

        PendingChangeManager.getInstance()
                .submit(new TerminateConnection(process1.getUserId(), connection, connection.getEndpoint1()));
        PendingChangeManager.getInstance()
                .submit(new TerminateConnection(process1.getUserId(), connection, connection.getEndpoint2()));

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
            this.terminateConnection(connection);
        }
    }

}
