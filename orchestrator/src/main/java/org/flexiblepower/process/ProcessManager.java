/**
 * File ProcessManager.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.process;

import java.util.List;

import org.bson.types.ObjectId;
import org.flexiblepower.connectors.MongoDbConnector;
import org.flexiblepower.model.Connection;
import org.flexiblepower.model.PrivateNode;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.Process.Parameter;
import org.flexiblepower.model.Process.ProcessState;
import org.flexiblepower.model.User;
import org.flexiblepower.orchestrator.NodeManager;
import org.flexiblepower.orchestrator.ServiceManager;
import org.flexiblepower.orchestrator.UserManager;
import org.flexiblepower.orchestrator.pendingchange.PendingChangeManager;

import lombok.extern.slf4j.Slf4j;

/**
 * ProcessManager
 *
 * @author wilco
 * @version 0.1
 * @since May 29, 2017
 */
@Slf4j
public class ProcessManager {

    private static ProcessManager instance = null;

    private final MongoDbConnector mongoDbConnector = MongoDbConnector.getInstance();
    private final UserManager userManager = UserManager.getInstance();

    private ProcessManager() {
    }

    public synchronized static ProcessManager getInstance() {
        if (ProcessManager.instance == null) {
            ProcessManager.instance = new ProcessManager();
        }
        return ProcessManager.instance;
    }

    public Process getProcess(final ObjectId processId) {
        return this.mongoDbConnector.get(Process.class, processId);
    }

    /**
     * @return List of processes of all users
     */
    public List<Process> listProcesses() {
        return this.listProcesses(null);
    }

    /**
     * @return List of processes of a specific user
     */
    public List<Process> listProcesses(final User owner) {
        if (owner == null) {
            return this.mongoDbConnector.list(Process.class);
        } else {
            return this.mongoDbConnector.listProcessesForUser(owner);
        }
    }

    public Process createProcess(final Process process) {
        if (process.getId() != null) {
            throw new IllegalArgumentException("A new process cannot have an identifier");
        }
        this.validateProcess(process);

        // Save with starting state and save
        process.setState(ProcessState.STARTING);
        MongoDbConnector.getInstance().save(process);

        // Submit PendingChange to actually start it
        PendingChangeManager.getInstance().submit(new CreateProcess.CreateDockerService(process));

        return process;
    }

    /**
     * @param process
     * @throws
     */
    private void validateProcess(final Process process) {
        if (process.getUserId() == null) {
            throw new NullPointerException("userId cannot be null");
        } else if (this.userManager.getUser(process.getUserId()) == null) {
            throw new IllegalArgumentException("Could not find user");
        } else if (process.getServiceId() == null) {
            throw new NullPointerException("serviceId cannot be null");
        } else if (ServiceManager.getInstance().getService(process.getServiceId()) == null) {
            throw new IllegalArgumentException("Could not find service");
        } else if (process.getNodePoolId() != null) {
            if (process.getPrivateNodeId() != null) {
                throw new IllegalArgumentException("Either the nodepool or the privatenode should be set");
            } else if (NodeManager.getInstance().getNodePool(process.getNodePoolId()) == null) {
                throw new IllegalArgumentException("Could not find NodePool");
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
    }

    public void deleteProcess(final Process process) {
        // Start two pendingchanges. The second one has a delay of 5000ms.
        PendingChangeManager.getInstance().submit(new TerminateProcess.SendTerminateSignal(process));
        PendingChangeManager.getInstance().submit(new TerminateProcess.RemoveDockerService(process));
    }

    public void updateProcess(final Process newProcess) {
        this.validateProcess(newProcess);

        final Process currentProcess = MongoDbConnector.getInstance().get(Process.class, newProcess.getId());

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
        if (!newProcess.getConfiguration().equals(currentProcess.getConfiguration())) {
            this.updateConfiguration(currentProcess, newProcess.getConfiguration());
        }
    }

    /**
     * @param currentProcess
     * @param newProcess
     * @return
     */
    private void moveProcess(final Process currentProcess, final Process newProcess) {
        final PendingChangeManager pcm = PendingChangeManager.getInstance();

        // Suspend connections
        for (final Connection c : ConnectionManager.getInstance().getConnectionsForProcess(currentProcess)) {
            pcm.submit(new MoveProcess.SupsendConnection(currentProcess.getUserId(), c, c.getEndpoint1()));
            pcm.submit(new MoveProcess.SupsendConnection(currentProcess.getUserId(), c, c.getEndpoint2()));
        }

        // Suspend process. This PendingChange will start all other PendingChanges.
        pcm.submit(new MoveProcess.SupsendProcess(currentProcess,
                newProcess.getNodePoolId(),
                newProcess.getPrivateNodeId()));
    }

    /**
     * @param process
     * @param newConfiguration
     * @return
     */
    private void updateConfiguration(final Process process, final List<Parameter> newConfiguration) {
        final ChangeProcessConfiguration pendingChange = new ChangeProcessConfiguration(process.getUserId(),
                process.getId(),
                newConfiguration);
        PendingChangeManager.getInstance().submit(pendingChange);
    }

}
