/**
 * File ServiceManager.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator;

import java.util.LinkedList;
import java.util.List;

import org.flexiblepower.connectors.RegistryConnector;
import org.flexiblepower.exceptions.RepositoryNotFoundException;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.Interface;
import org.flexiblepower.model.Service;

/**
 * ServiceManager
 *
 * @author wilco
 * @version 0.1
 * @since May 22, 2017
 */
public class ServiceManager {

    public final static String SERVICE_REPOSITORY = "services";

    private static ServiceManager instance = null;
    private final RegistryConnector registryConnectior;

    private ServiceManager() {
        this.registryConnectior = RegistryConnector.getInstance();
    }

    public synchronized static ServiceManager getInstance() {
        if (ServiceManager.instance == null) {
            ServiceManager.instance = new ServiceManager();
        }
        return ServiceManager.instance;
    }

    public List<String> listRepositories() {
        return this.registryConnectior.listRepositories();
    }

    public Service getService(final String id) {
        try {
            return this.registryConnectior.getService(ServiceManager.SERVICE_REPOSITORY, id);
        } catch (final RepositoryNotFoundException e) {
            // Can't happen
            return null;
        } catch (final ServiceNotFoundException e) {
            // Can happen
            return null;
        }
    }

    public List<Service> listServices() {
        try {
            return this.registryConnectior.listServices(ServiceManager.SERVICE_REPOSITORY);
        } catch (final RepositoryNotFoundException e) {
            // Can't happen
            return null;
        }
    }

    /**
     * @return
     */
    public List<Interface> listInterfaces() {
        final List<Interface> result = new LinkedList<>();
        for (final Service s : this.listServices()) {
            result.addAll(s.getInterfaces());
        }
        return result;
    }

    public Interface getInterfaceById(final String id) {
        for (final Interface i : this.listInterfaces()) {
            if (i.getId().equals(id)) {
                return i;
            }
        }
        return null;
    }

}
