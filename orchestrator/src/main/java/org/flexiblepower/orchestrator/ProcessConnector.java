/**
 * File ConnectionManager.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bson.types.ObjectId;
import org.flexiblepower.exceptions.ConnectionException;
import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.Connection;
import org.flexiblepower.model.Interface;
import org.flexiblepower.model.InterfaceVersion;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.Process.Parameter;
import org.flexiblepower.model.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * ConnectionManager
 *
 * @author coenvl
 * @version 0.1
 * @since Apr 19, 2017
 */
@Slf4j
public class ProcessConnector {

    private static ProcessConnector instance = null;

    private final Map<ObjectId, ProcessConnection> connections = new HashMap<>();

    private ProcessConnector() {
    }

    public static ProcessConnector getInstance() {
        if (ProcessConnector.instance == null) {
            ProcessConnector.instance = new ProcessConnector();
        }
        return ProcessConnector.instance;
    }

    private ProcessConnection getProcessConnection(final ObjectId processId) {
        if (!this.connections.containsKey(processId)) {
            this.connections.put(processId, new ProcessConnection(processId));
        }
        return this.connections.get(processId);
    }

    /**
     * @param connection
     * @return
     * @throws ProcessNotFoundException
     * @throws IOException
     * @throws ServiceNotFoundException
     */
    public boolean addConnection(final Connection connection)
            throws ProcessNotFoundException, ConnectionException, ServiceNotFoundException {
        final Process process1 = ProcessManager.getInstance().getProcess(connection.getProcess1());
        final ProcessConnection pc1 = this.getProcessConnection(process1.getId());
        final Process process2 = ProcessManager.getInstance().getProcess(connection.getProcess2());
        final ProcessConnection pc2 = this.getProcessConnection(process2.getId());

        final Service service1 = ServiceManager.getInstance().getService(process1.getServiceId());
        final Service service2 = ServiceManager.getInstance().getService(process2.getServiceId());

        final Interface interface1 = service1.getInterface(connection.getInterface1());
        final Interface interface2 = service2.getInterface(connection.getInterface2());

        for (final InterfaceVersion version1 : interface1.getInterfaceVersions()) {
            for (final InterfaceVersion version2 : interface2.getInterfaceVersions()) {
                if (version1.getReceivesHash().equals(version2.getSendsHash())
                        && version2.getReceivesHash().equals(version1.getSendsHash())) {

                    final int port1 = 5000 + new Random().nextInt(5000);
                    final int port2 = 5000 + new Random().nextInt(5000);
                    // TODO maybe random is not the best strategy?

                    pc1.setUpConnection(connection.getId(),
                            port1,
                            version1.getSendsHash(),
                            process2.getId().toString(),
                            port2,
                            version2.getReceivesHash());

                    pc2.setUpConnection(connection.getId(),
                            port2,
                            version2.getSendsHash(),
                            process1.getId().toString(),
                            port1,
                            version1.getReceivesHash());

                    return true;
                }
            }
        }

        return false;
    }

    public void processConnectionTerminated(final ObjectId processId) {
        this.connections.remove(processId);
    }

    /**
     * @param id
     */
    public void initNewProcess(final ObjectId processId) {
        final ProcessConnection processConnection = this.getProcessConnection(processId);
        processConnection.startProcess();
    }

    /**
     * @param id
     */
    public void terminate(final ObjectId processId) {
        final ProcessConnection processConnection = this.getProcessConnection(processId);
        processConnection.terminateProcess();
    }

    /**
     * @param id
     * @param configuration
     * @return
     */
    public Process updateConfiguration(final ObjectId processId, final List<Parameter> configuration) {
        final Process process = MongoDbConnector.getInstance().get(Process.class, processId);
        process.setConfiguration(configuration);
        MongoDbConnector.getInstance().save(process);
        this.getProcessConnection(processId).updateConfiguration();
        return process;
    }

}
