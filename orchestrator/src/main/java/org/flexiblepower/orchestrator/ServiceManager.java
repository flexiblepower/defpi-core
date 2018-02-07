/**
 * File ServiceManager.java
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
package org.flexiblepower.orchestrator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.flexiblepower.connectors.RegistryConnector;
import org.flexiblepower.exceptions.RepositoryNotFoundException;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.Interface;
import org.flexiblepower.model.Service;

import com.google.common.collect.ImmutableList;

/**
 * ServiceManager
 *
 * @version 0.1
 * @since May 22, 2017
 */
@SuppressWarnings("static-method")
public class ServiceManager {

    public final static String SERVICE_REPOSITORY = "services";

    private static ServiceManager instance = null;

    private ServiceManager() {
    }

    public synchronized static ServiceManager getInstance() {
        if (ServiceManager.instance == null) {
            ServiceManager.instance = new ServiceManager();
        }
        return ServiceManager.instance;
    }

    public Service getService(final String id) throws ServiceNotFoundException {
        return RegistryConnector.getInstance().getService(ServiceManager.SERVICE_REPOSITORY, id);
    }

    public List<Service> listServices() {
        try {
            return ImmutableList.copyOf(RegistryConnector.getInstance().getServices(ServiceManager.SERVICE_REPOSITORY));
        } catch (final RepositoryNotFoundException e) {
            // Can't happen
            return null;
        }
    }

    /**
     * Returns a list of all interfaces that are currently known.
     *
     * @return a List of interfaces
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

}
