/**
 * File ProcessManager.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.bson.types.ObjectId;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.Process.ProcessState;
import org.flexiblepower.model.User;

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

    private final DockerConnector dockerConnector = DockerConnector.getInstance();
    private final MongoDbConnector mongoDbConnector = MongoDbConnector.getInstance();
    private final ProcessConnector processConnector = ProcessConnector.getInstance();
    private final UserManager userManager = UserManager.getInstance();
    private final ScheduledExecutorService threadpool = Executors.newScheduledThreadPool(8);

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

        // This is a valid state, create the database record
        process.setState(ProcessState.STARTING);
        this.mongoDbConnector.save(process);

        this.threadpool.execute(() -> {
            // Now create the process in Docker
            final User user = this.userManager.getUser(process.getUserId());
            final String dockerId = DockerConnector.getInstance().newProcess(process, user);

            process.setState(ProcessState.INITIALIZING);
            process.setDockerId(dockerId);
            this.mongoDbConnector.save(process);

            ProcessManager.this.threadpool.execute(() -> {
                // Create management connection
                ProcessManager.log.info("Going to configure process " + process.getId());
                ProcessConnector.getInstance().initNewProcess(process.getId());
            });

        });

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
            } else if (NodeManager.getInstance().getPrivateNode(process.getPrivateNodeId()) == null) {
                throw new IllegalArgumentException("Could not find NodePool");
            }
        } else if ((process.getPrivateNodeId() == null) && (process.getNodePoolId() == null)) {
            throw new IllegalArgumentException("Either the nodepool or the privatenode should be set");
        }
    }

    public void deleteProcess(final Process process) {
        // Notify process
        this.threadpool.execute(() -> {
            try {
                ProcessConnector.getInstance().terminate(process.getId());
            } catch (final Exception e) {
                // If notification doesn't work, that's too bad, make sure it gets removed anyway
                ProcessManager.log.warn("Could not notify process it is going to terminate. Terminating anyway.", e);
            }
        });

        // Now give it some time to shut down
        this.threadpool.schedule(() -> {
            // Delete Docker service
            try {
                this.dockerConnector.removeProcess(process);
            } catch (final Exception e) {
                // That's fine, we didn't want it anyway
            }
            // Delete record from MongoDB
            this.mongoDbConnector.delete(process);
        }, 5, TimeUnit.SECONDS);
    }

    public Process updateProcess(final Process newProcess) {
        this.validateProcess(newProcess);

        Process currentProcess = MongoDbConnector.getInstance().get(Process.class, newProcess.getId());

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
            currentProcess = this.moveProcess(currentProcess, newProcess);
        } else {
            // Did the configuration change?
            if (!newProcess.getConfiguration().equals(currentProcess.getConfiguration())) {
                currentProcess = this.updateConfiguration(currentProcess, newProcess);
            }
        }

        return currentProcess;
    }

    /**
     * @param currentProcess
     * @param newProcess
     * @return
     */
    private Process moveProcess(final Process currentProcess, final Process newProcess) {
        final Process updatedProcess = currentProcess;
        updatedProcess.setConfiguration(newProcess.getConfiguration());
        updatedProcess.setPrivateNodeId(newProcess.getPrivateNodeId());
        updatedProcess.setNodePoolId(newProcess.getNodePoolId());
        // write change to database
        this.mongoDbConnector.save(updatedProcess);

        this.threadpool.execute(() -> {
            // Tell the process to suspend
            final byte[] suspendProcess = this.processConnector.suspendProcess(currentProcess.getId());
            // Remove the Docker service
            try {
                this.dockerConnector.removeProcess(currentProcess);
            } catch (final Exception e) {
                ProcessManager.log
                        .error("Could not remove Docker Service when moving process " + currentProcess.getId(), e);
            }
            // Create a new Docker Service
            this.dockerConnector.newProcess(updatedProcess, UserManager.getInstance().getUser(newProcess.getUserId()));
            // Tell the new process to resume
            this.processConnector.resume(updatedProcess.getId(), suspendProcess);
        });
        return updatedProcess;
    }

    /**
     * @param currentProcess
     * @param newProcess
     * @return
     */
    private Process updateConfiguration(final Process currentProcess, final Process newProcess) {
        return this.processConnector.updateConfiguration(currentProcess.getId(), newProcess.getConfiguration());
    }

}
