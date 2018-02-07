/**
 * File ConnectionManager.java
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
package org.flexiblepower.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.bson.types.ObjectId;
import org.flexiblepower.connectors.MongoDbConnector;
import org.flexiblepower.exceptions.ConnectionException;
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
import org.flexiblepower.orchestrator.UserManager;
import org.flexiblepower.orchestrator.pendingchange.PendingChangeManager;

import lombok.extern.slf4j.Slf4j;

/**
 * ConnectionManager
 *
 * @version 0.1
 * @since Jul 24, 2017
 */
@Slf4j
@SuppressWarnings("static-method")
public class ConnectionManager {

    private static ConnectionManager instance;

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
        return MongoDbConnector.getInstance().list(Connection.class);
    }

    /**
     * Returns the connection that is stored in the database with the provided id, or null if no such connection exists.
     *
     * @param connectionId
     * @return the connection that has the provided id, or null
     * @throws InvalidObjectIdException
     */
    public Connection getConnection(final ObjectId connectionId) {
        return MongoDbConnector.getInstance().get(Connection.class, connectionId);
    }

    /**
     * Removes the connection that is stored in the database with the provided id
     *
     * @param connectionId
     * @return the connection that has the provided id, or null
     * @throws InvalidObjectIdException
     */
    public void deleteConnection(final Connection connection) {
        MongoDbConnector.getInstance().delete(connection);
    }

    /**
     * Insert the provided connection in the database.
     *
     * @param connection
     * @return the id of the newly inserted connection
     * @throws ServiceNotFoundException
     * @throws ProcessNotFoundException
     * @throws ConnectionException
     */
    public void createConnection(final Connection connection) throws ProcessNotFoundException,
            ServiceNotFoundException,
            ConnectionException {
        ConnectionManager.validateConnection(connection);
        ConnectionManager.validateMultipleConnect(connection);

        ConnectionManager.setPort(connection);
        ConnectionManager.setInterfaceNames(connection);

        final Process process1 = ProcessManager.getInstance().getProcess(connection.getEndpoint1().getProcessId());
        MongoDbConnector.getInstance().save(connection);

        PendingChangeManager.getInstance()
                .submit(new CreateConnectionEndpoint(process1.getUserId(), connection, connection.getEndpoint1()));
        PendingChangeManager.getInstance()
                .submit(new CreateConnectionEndpoint(process1.getUserId(), connection, connection.getEndpoint2()));
    }

    private static void validateMultipleConnect(final Connection newConnection) throws ProcessNotFoundException,
            ServiceNotFoundException,
            ConnectionException {
        ConnectionManager.validateMultipleConnect(newConnection.getEndpoint1());
        ConnectionManager.validateMultipleConnect(newConnection.getEndpoint2());
    }

    private static void validateMultipleConnect(final Connection.Endpoint endpoint) throws ServiceNotFoundException,
            ProcessNotFoundException,
            ConnectionException {
        final Process process = ProcessManager.getInstance().getProcess(endpoint.getProcessId());
        final Service service = ServiceManager.getInstance().getService(process.getServiceId());
        final Interface intface = service.getInterface(endpoint.getInterfaceId());
        if (!intface.isAllowMultiple()) {
            // Is there already a connection for this interface?
            for (final Connection c : ConnectionManager.getInstance().getConnectionsForProcess(process)) {
                if (c.getEndpointForProcess(process).getInterfaceId().equals(intface.getId())) {
                    throw new ConnectionException("Connot create new connection for process " + process.getId()
                            + " for interface " + intface.getId()
                            + ". The process does not allow multiple connections of the same type.");
                }
            }
        }
    }

    private static void setInterfaceNames(final Connection connection) {
        final Interface interface1 = ServiceManager.getInstance()
                .getInterfaceById(connection.getEndpoint1().getInterfaceId());
        final Interface interface2 = ServiceManager.getInstance()
                .getInterfaceById(connection.getEndpoint2().getInterfaceId());
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

    private static void setPort(final Connection connection) {
        connection.setPort(5000 + new Random().nextInt(5000));
    }

    private static void validateConnection(final Connection c) {
        if ((c.getEndpoint1().getProcessId() == null) || (c.getEndpoint2().getProcessId() == null)) {
            throw new IllegalArgumentException("ProcessId cannot be null");
        }

        if (c.getEndpoint1().getProcessId().equals(c.getEndpoint2().getProcessId())) {
            throw new IllegalArgumentException("A process cannot connect with itself");
        }

        if ((c.getEndpoint1().getInterfaceId() == null) || c.getEndpoint1().getInterfaceId().isEmpty()
                || (c.getEndpoint2().getInterfaceId() == null) || c.getEndpoint2().getInterfaceId().isEmpty()) {
            throw new IllegalArgumentException("Interface identifier cannot be empty");
        }

        try {
            final Process process1 = ProcessManager.getInstance().getProcess(c.getEndpoint1().getProcessId());
            final Process process2 = ProcessManager.getInstance().getProcess(c.getEndpoint2().getProcessId());

            final Service service1 = ServiceManager.getInstance().getService(process1.getServiceId());
            final Service service2 = ServiceManager.getInstance().getService(process2.getServiceId());

            final Interface if1 = service1.getInterface(c.getEndpoint1().getInterfaceId());
            final Interface if2 = service2.getInterface(c.getEndpoint2().getInterfaceId());

            if (if1 == null) {
                throw new IllegalArgumentException("The Service of Process 1 with id " + c.getEndpoint1().getProcessId()
                        + " does not contain the interface " + c.getEndpoint1().getInterfaceId());
            }

            if (if2 == null) {
                throw new IllegalArgumentException("The Service of Process 2 with id " + c.getEndpoint2().getProcessId()
                        + " does not contain the interface " + c.getEndpoint2().getInterfaceId());
            }

            if (!process1.getUserId().equals(process2.getUserId())) {
                if (!(process1.getServiceId().equals(ProcessManager.getDashboardGatewayServiceId())
                        || process2.getServiceId().equals(ProcessManager.getDashboardGatewayServiceId()))) {
                    // Dashboard-gateway is the only exception to the rule that processes can only connect if they are
                    // owned by the same user
                    throw new IllegalArgumentException("The two processes are not owned by the same user");
                }

            }

            if (!if1.isCompatibleWith(if2)) {
                throw new IllegalArgumentException("Interface " + c.getEndpoint1().getInterfaceId() + " and interface "
                        + c.getEndpoint2().getInterfaceId() + " are not compatible with each other");
            }

        } catch (final ServiceNotFoundException | ProcessNotFoundException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Removes the connection that has the provided id from the database.
     *
     * @param connection
     * @throws ProcessNotFoundException
     * @throws InvalidObjectIdException
     */
    public void terminateConnection(final Connection connection) throws ProcessNotFoundException {
        final Process process1 = ProcessManager.getInstance().getProcess(connection.getEndpoint1().getProcessId());

        PendingChangeManager.getInstance()
                .submit(new TerminateConnection(process1.getUserId(), connection, connection.getEndpoint1()));
        PendingChangeManager.getInstance()
                .submit(new TerminateConnection(process1.getUserId(), connection, connection.getEndpoint2()));

        MongoDbConnector.getInstance().delete(connection);
    }

    /**
     * @return a list of all connections that are connected to the process with the provided id
     */
    public List<Connection> getConnectionsForProcess(final Process process) {
        return MongoDbConnector.getInstance().getConnectionsForProcess(process);
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
     * @throws ProcessNotFoundException
     */
    public synchronized void deleteConnectionsForProcess(final Process process) throws ProcessNotFoundException {
        for (final Connection connection : this.getConnectionsForProcess(process)) {
            this.terminateConnection(connection);
        }
    }

    public void createAutoConnectConnections(final Process process) throws ServiceNotFoundException {
        final Service service = ServiceManager.getInstance().getService(process.getServiceId());
        final User user = UserManager.getInstance().getUser(process.getUserId());
        for (final Interface intface : service.getInterfaces()) {
            if (intface.isAutoConnect()) {
                // search for other processes with that interface
                final Process dashboardGateway = ProcessManager.getInstance().getDashboardGateway();
                List<Process> processesToConnectWith;
                if (process.getServiceId().equals(ProcessManager.getDashboardGatewayServiceId())) {
                    // This is the dashboard-gateway. It can connect with the dashboard process from every user.
                    processesToConnectWith = ProcessManager.getInstance().listProcesses();
                } else {
                    // This is a normal process. It can connect with processes of this user.
                    processesToConnectWith = ProcessManager.getInstance().listProcesses(user);
                }
                if (dashboardGateway != null) {
                    // If there is a dashboard-gateway, that is also a process that could be connected
                    final List<Process> newList = new ArrayList<>();
                    newList.addAll(processesToConnectWith);
                    newList.add(dashboardGateway);
                    processesToConnectWith = newList;
                }
                for (final Process otherProcess : processesToConnectWith) {
                    if (process.getId().equals(otherProcess.getId())) {
                        // It should not connect with itself
                        continue;
                    }
                    try {
                        final Service otherService = ServiceManager.getInstance()
                                .getService(otherProcess.getServiceId());
                        for (final Interface otherIntface : otherService.getInterfaces()) {
                            if (otherIntface.isAutoConnect() && intface.isCompatibleWith(otherIntface)) {
                                // They can autoconnect!
                                final Connection.Endpoint ep1 = new Connection.Endpoint(process.getId(),
                                        intface.getId());
                                final Connection.Endpoint ep2 = new Connection.Endpoint(otherProcess.getId(),
                                        otherIntface.getId());
                                final Connection connection = new Connection(null, ep1, ep2);
                                try {
                                    ConnectionManager.getInstance().createConnection(connection);
                                } catch (final ProcessNotFoundException e) {
                                    ConnectionManager.log.warn(
                                            "Could not find process while creating an autoconnect connection for interface "
                                                    + intface.getId() + ", ignoring.");
                                } catch (final ConnectionException e) {
                                    // This new connection violates one of the constraints. So its not suited for
                                    // autoconnect, no problem...
                                }
                            }
                        }
                    } catch (final ServiceNotFoundException e) {
                        ConnectionManager.log.warn(
                                "Could not find the service of the process of a user, skipping it while searching for autoconnect interfaces.");
                    }
                }
            }
        }
    }

}
