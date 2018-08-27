/*-
 * #%L
 * dEF-Pi REST Orchestrator
 * %%
 * Copyright (C) 2017 - 2018 Flexible Power Alliance Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.flexiblepower.process;

import java.util.List;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.flexiblepower.connectors.MongoDbConnector;
import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.Connection;
import org.flexiblepower.model.NodePool;
import org.flexiblepower.model.PrivateNode;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.Process.ProcessParameter;
import org.flexiblepower.model.Process.ProcessState;
import org.flexiblepower.model.User;
import org.flexiblepower.orchestrator.NodeManager;
import org.flexiblepower.orchestrator.ServiceManager;
import org.flexiblepower.orchestrator.UserManager;
import org.flexiblepower.orchestrator.pendingchange.PendingChangeManager;

/**
 * The ProcessManager is used by the orchestrator to create, update and list the processes from different users.
 *
 * @version 0.1
 * @since May 29, 2017
 */
@SuppressWarnings("static-method")
public class ProcessManager {

    /**
     * The environment variable that holds the hostname of the node that should serve the dashboard gateway.
     * The default hostname is null, which means a random node is chosen.
     */
    public static final String DASHBOARD_GATEWAY_HOSTNAME_KEY = "DASHBOARD_GATEWAY_HOSTNAME";

    /**
     * The environment variable that holds the port at which the dashboard gateway should be published
     */
    public static final String DASHBOARD_GATEWAY_PORT_KEY = "DASHBOARD_GATEWAY_PORT";
    private static final int DASHBOARD_GATEWAY_PORT_DFLT = 8080;

    /**
     * The environment variable that holds service id of the dashboard gateway
     */
    public static final String DASHBOARD_GATEWAY_SERVICE_ID_KEY = "DASHBOARD_GATEWAY_SERVICE_ID";
    private static final String DASHBOARD_GATEWAY_SERVICE_ID_DFLT = "dashboard-gateway";

    private static ProcessManager instance = null;

    private final MongoDbConnector mongoDbConnector = MongoDbConnector.getInstance();
    private final UserManager userManager = UserManager.getInstance();

    /*
     * Empty private contructor for singleton design pattern
     */
    private ProcessManager() {
    }

    /**
     * @return The singleton instance of the ProcessManager
     */
    public static ProcessManager getInstance() {
        if (ProcessManager.instance == null) {
            ProcessManager.instance = new ProcessManager();
        }
        return ProcessManager.instance;
    }

    /**
     * Returns the process that is stored in the database with the provided id, or throws an exception if no such
     * process exists.
     *
     * @param processId The ID of the process to retrieve
     * @return the process that has the provided id
     * @throws ProcessNotFoundException When no such process exists
     */
    public Process getProcess(final ObjectId processId) throws ProcessNotFoundException {
        final Process ret = this.mongoDbConnector.get(Process.class, processId);
        if (ret == null) {
            throw new ProcessNotFoundException(processId);
        }
        return ret;
    }

    /**
     * @return List of processes of all users
     */
    public List<Process> listProcesses() {
        return this.mongoDbConnector.list(Process.class);
    }

    /**
     * @param owner The user who is the owner of the list of processes
     * @return List of processes of a specific user
     */
    public List<Process> listProcessesForUser(final User owner) {
        return this.mongoDbConnector.listProcessesForUser(owner);
    }

    /**
     * Create a process according to the specified description. It must have at least the following fields:
     * <ul>
     * <li>User</li>
     * <li>Service id</li>
     * <li>Node allocation (either a private node or a nodepool)</li>
     * </ul>
     *
     * The ProcessManager will create the representation of the process, and a new pending change to create the docker
     * service. Eventually the service itself will trigger a new pending change to get the process configuration.
     *
     * @param process The (description of the) process to create
     * @return The created process with a valid ObjectId.
     */
    public Process createProcess(final Process process) {
        if (process.getId() != null) {
            throw new IllegalArgumentException("A new process cannot have an identifier");
        }
        this.validateProcess(process);

        // Create a random token for this process
        process.setToken(UUID.randomUUID().toString());

        // Does the process have a name? If not, think of one
        if ((process.getName() == null) || process.getName().isEmpty()) {
            process.setName(process.getServiceId() + " @"
                    + UserManager.getInstance().getUser(process.getUserId()).getUsername());
        }

        // Save with starting state and save
        process.setState(ProcessState.STARTING);
        MongoDbConnector.getInstance().save(process);

        // Submit PendingChange to actually start it
        PendingChangeManager.getInstance().submit(new CreateProcess.CreateDockerService(process));

        return process;
    }

    /**
     * Check if a process is created that acts as the dashboard gateway
     *
     * @return Whether a process with the service id specified in the {@value #DASHBOARD_GATEWAY_SERVICE_ID_KEY} exists
     */
    public boolean dashboardGatewayExists() {
        return this.getDashboardGateway() != null;
    }

    /**
     * Get the port number where the dashboard gateway should be published
     *
     * @return The number of the port as specified in {@value #DASHBOARD_GATEWAY_PORT_KEY} or if that is not set, the
     *         default port
     */
    public static int getDashboardGatewayPort() {
        final String portFromEnv = System.getenv(ProcessManager.DASHBOARD_GATEWAY_PORT_KEY);
        final int defaultPort = ProcessManager.DASHBOARD_GATEWAY_PORT_DFLT;
        if (portFromEnv != null) {
            try {
                return Integer.parseInt(portFromEnv);
            } catch (final NumberFormatException e) {
                // We keep it at the default
            }
        }
        return defaultPort;
    }

    /**
     * Get the service id of the dashboard gateway
     *
     * @return The id of the service as specified in {@value #DASHBOARD_GATEWAY_SERVICE_ID_KEY} or if that is not set,
     *         the default service id
     */
    public static String getDashboardGatewayServiceId() {
        final String gatewayService = System.getenv(ProcessManager.DASHBOARD_GATEWAY_SERVICE_ID_KEY);
        if (gatewayService == null) {
            return ProcessManager.DASHBOARD_GATEWAY_SERVICE_ID_DFLT;
        } else {
            return gatewayService;
        }
    }

    /**
     * Get the process that currently acts as the dashboard gateway, or null if no such process exists
     *
     * @return The process with the service id specified in the {@value #DASHBOARD_GATEWAY_SERVICE_ID_KEY}
     */
    public Process getDashboardGateway() {
        for (final Process p : this.listProcesses()) {
            if (p.getServiceId().equals(ProcessManager.getDashboardGatewayServiceId())) {
                return p;
            }
        }
        return null;
    }

    /**
     * Make sure the process is validate for creation or updating. Checks the required fields, and throws an exception
     * if the process is invalid.
     *
     * @param process The process to validate
     * @throws IllegalArgumentException when required fields are missing, or referenced objectIds are not found
     */
    private void validateProcess(final Process process) {
        // Validate user
        if (process.getUserId() == null) {
            throw new NullPointerException("userId cannot be null");
        } else if (this.userManager.getUser(process.getUserId()) == null) {
            throw new IllegalArgumentException("Could not find user");
        }

        // Validate service
        if (process.getServiceId() == null) {
            throw new NullPointerException("serviceId cannot be null");
        } else {
            try {
                ServiceManager.getInstance().getService(process.getServiceId());
            } catch (final ServiceNotFoundException e) {
                throw new IllegalArgumentException("Could not find service");
            }
        }

        // Validate node allocation
        if (process.getNodePoolId() != null) {
            if (process.getPrivateNodeId() != null) {
                throw new IllegalArgumentException("Either the nodepool or the privatenode should be set");
            } else {
                final NodePool nodepool = NodeManager.getInstance().getNodePool(process.getNodePoolId());
                if (nodepool == null) {
                    throw new IllegalArgumentException("Could not find NodePool");
                } else if (NodeManager.getInstance().getPublicNodesInNodePool(nodepool).isEmpty()) {
                    throw new IllegalArgumentException("Cannot assign a Process to an empty NodePool");
                }
            }
        } else if (process.getPrivateNodeId() != null) {
            if (process.getNodePoolId() != null) {
                throw new IllegalArgumentException("Either the nodepool or the privatenode should be set");
            } else {
                final PrivateNode privateNode = NodeManager.getInstance().getPrivateNode(process.getPrivateNodeId());
                if (privateNode == null) {
                    throw new IllegalArgumentException("Could not find Private Node");
                } else if (!privateNode.getUserId().equals(process.getUserId())) {
                    throw new IllegalArgumentException(
                            "The Process cannot be assigned to the private node of another user");
                }
            }
        } else if ((process.getPrivateNodeId() == null) && (process.getNodePoolId() == null)) {
            throw new IllegalArgumentException("Either the nodepool or the privatenode should be set");
        }

        // Is there a duplicate dashboard gateway?
        if (process.getServiceId().equals(ProcessManager.getDashboardGatewayServiceId())) {
            if (this.dashboardGatewayExists()) {
                throw new IllegalArgumentException("There can only be one Dashboard Gateway in the system");
            }
            if (!this.userManager.getUser(process.getUserId()).isAdmin()) {
                throw new IllegalArgumentException("Only administrators can instantiate the dashboard gateway");
            }
        }
    }

    /**
     * Delete the process from the dEF-Pi environment. This is done by first removing all connections, then terminating
     * the process, and finally removing it from the database.
     *
     * @param process The process to delete
     */
    public void deleteProcess(final Process process) {
        ConnectionManager.getInstance().deleteConnectionsForProcess(process);

        // Start two pendingchanges. The first one has a delay of 2000ms and the second one has a delay of 5000ms.
        PendingChangeManager.getInstance().submit(new TerminateProcess.SendTerminateSignal(process));
        PendingChangeManager.getInstance().submit(new TerminateProcess.RemoveDockerService(process));
    }

    /**
     * Update a process according to the specified description. It must have an objectId which points to an existing
     * process. Note that a process cannot be assigned to a different user, and this function will throw an exception if
     * this is attempted.
     *
     * @param newProcess The (description of the) process to update
     * @throws IllegalArgumentException if the updated process is invalid, or has a different user than the original
     *             process.
     */
    public void updateProcess(final Process newProcess) {
        this.validateProcess(newProcess);

        final Process currentProcess = MongoDbConnector.getInstance().get(Process.class, newProcess.getId());
        // Rename the process?
        if (!newProcess.getName().equals(currentProcess.getName())) {
            currentProcess.setName(newProcess.getName());
            MongoDbConnector.getInstance().save(currentProcess);
        }

        if (!newProcess.getUserId().equals(currentProcess.getUserId())) {
            throw new IllegalArgumentException("A process cannot be assigned to a different user");
        }

        // Should the process be moved?
        boolean move = false;
        if (newProcess.getNodePoolId() != null) {
            // this should run on a public node
            if (!newProcess.getNodePoolId().equals(currentProcess.getNodePoolId())) {
                move = true;
            }
        } else {
            // this should run on a private node
            if (!newProcess.getPrivateNodeId().equals(currentProcess.getPrivateNodeId())) {
                move = true;
            }
        }
        if (move) {
            this.moveProcess(currentProcess, newProcess);
        }

        // Did the configuration change?
        if ((newProcess.getConfiguration() != null)
                && !newProcess.getConfiguration().equals(currentProcess.getConfiguration())) {
            this.updateConfiguration(currentProcess, newProcess.getConfiguration());
        }
    }

    /**
     * @param currentProcess
     * @param newProcess
     */
    private void moveProcess(final Process currentProcess, final Process newProcess) {
        final PendingChangeManager pcm = PendingChangeManager.getInstance();

        // Suspend connections
        for (final Connection c : ConnectionManager.getInstance().getConnectionsForProcess(currentProcess)) {
            pcm.submit(new MoveProcess.SuspendConnection(currentProcess.getUserId(), c, c.getEndpoint1()));
            pcm.submit(new MoveProcess.SuspendConnection(currentProcess.getUserId(), c, c.getEndpoint2()));
        }

        // Suspend process. This PendingChange will start all other PendingChanges.
        pcm.submit(new MoveProcess.SuspendProcess(currentProcess,
                newProcess.getNodePoolId(),
                newProcess.getPrivateNodeId()));
    }

    /**
     * Update the configuration for a process
     *
     * @param process The process to update
     * @param newConfiguration A list of parameters representing the new configuration
     */
    private void updateConfiguration(final Process process, final List<ProcessParameter> newConfiguration) {
        final ChangeProcessConfiguration pendingChange = new ChangeProcessConfiguration(process, newConfiguration);
        PendingChangeManager.getInstance().submit(pendingChange);
    }

    /**
     * Trigger a process by sending its configuration and/or connections.
     *
     * @param process The process to trigger
     */
    public void triggerConfig(final Process process) {
        final Process currentProcess = MongoDbConnector.getInstance().get(Process.class, process.getId());
        currentProcess.setState(ProcessState.INITIALIZING);
        MongoDbConnector.getInstance().save(currentProcess);

        final PendingChangeManager pcm = PendingChangeManager.getInstance();
        pcm.submit(new CreateProcess.SendConfiguration(currentProcess));

        for (final Connection c : ConnectionManager.getInstance().getConnectionsForProcess(currentProcess)) {
            pcm.submit(new CreateConnectionEndpoint(currentProcess.getUserId(),
                    c,
                    c.getEndpointForProcess(currentProcess)));
        }
    }

    /**
     * Find a process that is identified by a token.
     *
     * @param token the token that should uniquely identify this process
     * @return The process that is identified by this token.
     */
    public Process getProcessByToken(final String token) {
        return this.mongoDbConnector.getProcessByToken(token);
    }

}
