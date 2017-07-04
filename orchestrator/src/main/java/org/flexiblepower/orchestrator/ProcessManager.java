/**
 * File ProcessManager.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator;

import java.util.List;

import org.bson.types.ObjectId;
import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.Process.ProcessState;
import org.flexiblepower.model.User;

/**
 * ProcessManager
 *
 * @author wilco
 * @version 0.1
 * @since May 29, 2017
 */
public class ProcessManager {

    private static ProcessManager instance = null;

    private final DockerConnector dockerConnector = new DockerConnector();
    private final MongoDbConnector mongoDbConnector = MongoDbConnector.getInstance();

    private ProcessManager() {
    }

    public static ProcessManager getInstance() {
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

        // This is a valid state, create the database record
        process.setState(ProcessState.STARTING);
        this.mongoDbConnector.save(process);

        // Now create the process in Docker
        final User user = this.mongoDbConnector.getUser(process.getUserId());
        final String dockerId = this.dockerConnector.newProcess(process, user);

        process.setState(ProcessState.INITIALIZING);
        process.setDockerId(dockerId);
        this.mongoDbConnector.save(process);

        return process;
    }

    /**
     * @param process
     * @throws
     */
    private void validateProcess(final Process process) {
        if (process.getUserId() == null) {
            throw new NullPointerException("userId cannot be null");
        } else if (this.mongoDbConnector.getUser(process.getUserId()) == null) {
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
            } else if (NodeManager.getInstance().getPrivateNode(process.getPrivateNodeId()) == null) {
                throw new IllegalArgumentException("Could not find NodePool");
            }
        } else if ((process.getPrivateNodeId() == null) && (process.getNodePoolId() == null)) {
            throw new IllegalArgumentException("Either the nodepool or the privatenode should be set");
        }
    }

    public void deleteProcess(final Process process) {
        // TODO notify process
        // TODO update state
        try {
            this.dockerConnector.removeProcess(process);
        } catch (final ProcessNotFoundException e) {
            // That's fine, we didn't want it anyway
        }
        this.mongoDbConnector.delete(process);
    }

    public Process updateProcess(final Process process) {
        return null; // TODO
    }

}
