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
package org.flexiblepower.orchestrator;

import java.util.ArrayList;
import java.util.Collections;
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
 * @version 0.1
 * @since May 22, 2017
 */
@SuppressWarnings("static-method")
public class ServiceManager {

    /**
     * The name of the repository in the docker registry where all service can be found. This is, combined with the
     * location of the registry, the prefix for all services for the docker image name,
     */
    public final static String SERVICE_REPOSITORY = "services";

    private static ServiceManager instance = null;

    /*
     * Empty private contructor for singleton design pattern
     */
    private ServiceManager() {
    }

    /**
     * @return The singleton instance of the ServiceManager
     */
    public static ServiceManager getInstance() {
        if (ServiceManager.instance == null) {
            ServiceManager.instance = new ServiceManager();
        }
        return ServiceManager.instance;
    }

    /**
     * Get the service with the specified Id.
     *
     * @param id The id of the service to retrieve from the registry
     * @return The service with the specified Id
     * @throws ServiceNotFoundException if no service is found with the specified id
     */
    public Service getService(final String id) throws ServiceNotFoundException {
        return RegistryConnector.getInstance().getService(ServiceManager.SERVICE_REPOSITORY, id);
    }

    /**
     * List all existing services that are currently available in the registry. If no services are found, the empty list
     * is returned
     *
     * @return An immutable list of services in the service registry
     */
    public List<Service> listServices() {
        try {
            return RegistryConnector.getInstance().getServices(ServiceManager.SERVICE_REPOSITORY);
        } catch (final RepositoryNotFoundException e) {
            // Can't happen
            return Collections.emptyList();
        }
    }

    /**
     * Returns a list of all interfaces that are currently known. If there are no services in the registry, this list
     * will be empty
     *
     * @return a List of interfaces
     */
    public List<Interface> listInterfaces() {
        final List<Interface> result = new LinkedList<>();
        for (final Service s : this.listServices()) {
            for (final Interface i : s.getInterfaces()) {
                if (!result.contains(i)) {
                    result.add(i);
                }
            }
            // result.addAll(s.getInterfaces());
        }
        return result;
    }

    /**
     * Get the interface with the specified Id, or {@code null} if no such interface exists
     *
     * @param id The id of the interface to retrieve from the registry
     * @return The Interface with the specified Id
     */
    public Interface getInterfaceById(final String id) {
        for (final Interface i : this.listInterfaces()) {
            if (i.getId().equals(id)) {
                return i;
            }
        }
        return null;
    }

    /**
     * Returns a list of all services that have any interface that may connect with the provided interface.
     *
     * @param intface The interface to find compatibility for
     * @return a List of services that may be connected
     * @see Interface#isCompatibleWith(Interface)
     */
    public List<Service> getServicesThatCanConnectWith(final Interface intface) {
        final List<Service> result = new ArrayList<>();
        for (final Service s : this.listServices()) {
            for (final Interface otherIntf : s.getInterfaces()) {
                if (intface.isCompatibleWith(otherIntf)) {
                    result.add(s);
                    break; // go to the next service
                }
            }
        }
        return result;
    }

    /**
     * Get all versions of a service with the specified Id.
     *
     * @param id The id of the service to retrieve from the registry
     * @return The list of service versions with the specified Id
     * @throws ServiceNotFoundException if no service is found with the specified id
     */
    public List<Service> listServiceVersions(final String id) throws ServiceNotFoundException {
        return RegistryConnector.getInstance().getAllServiceVersions(ServiceManager.SERVICE_REPOSITORY, id);
    }

}
