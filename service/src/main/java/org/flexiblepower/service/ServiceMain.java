/**
 * File ServiceMain.java
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
package org.flexiblepower.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Set;

import org.flexiblepower.service.exceptions.ServiceInvocationException;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ServiceMain
 *
 * @version 0.1
 * @since Jul 24, 2017
 */
public final class ServiceMain {

    private static final Logger log = LoggerFactory.getLogger(ServiceMain.class);
    private static final ServiceExecutor serviceExecutor = ServiceExecutor.getInstance();
    private static Reflections reflections;
    protected static int threadCount = 0;

    /**
     * @return
     * @throws ServiceInvocationException
     */
    @SuppressWarnings("rawtypes")
    private static Class<? extends Service> getServiceClass() throws ServiceInvocationException {
        final Set<Class<? extends Service>> set = ServiceMain.reflections.getSubTypesOf(Service.class);

        // Must have exactly 1 result
        if (set.size() > 1) {
            throw new ServiceInvocationException(
                    "Unable to start service, more than 1 service implementations found: " + set.toString());
        } else if (set.isEmpty()) {
            throw new ServiceInvocationException("Unable to start service, no service implementations found");
        }

        return set.iterator().next();
    }

    /**
     * Registers all connectionHandlerFactories for their corresponding connectionHandlers.
     *
     * @param service
     */
    private static void registerMessageHandlers(final Service<?> service) {
        final Set<Class<? extends ConnectionHandlerManager>> managers = ServiceMain.reflections
                .getSubTypesOf(ConnectionHandlerManager.class);

        for (final Class<? extends ConnectionHandlerManager> managerClass : managers) {
            if (managerClass.isInterface()) {
                // We found the generated interface, but we want the implementation
                continue;
            }

            final ConnectionHandlerManager manager = ServiceMain.instantiateManagerWithService(managerClass, service);
            if (manager == null) {
                // We failed to build a manager for this interface...
                continue;
            }

            try {
                for (final Method method : managerClass.getMethods()) {
                    if (method.getName().startsWith("build")
                            && ConnectionHandler.class.isAssignableFrom(method.getReturnType())
                            && (method.getParameterCount() == 1)
                            && Connection.class.isAssignableFrom(method.getParameterTypes()[0])) {
                        // We found a builder
                        @SuppressWarnings("unchecked")
                        final Class<? extends ConnectionHandler> handlerClass = (Class<? extends ConnectionHandler>) method
                                .getReturnType();
                        ConnectionManager.registerConnectionHandlerFactory(handlerClass, manager);
                    }
                }
            } catch (final Exception e) {
                // Try and continue with the next interface
                ServiceMain.log.warn("Unable to instantiate manager type {} of service {}: {}",
                        managerClass,
                        service,
                        e.getMessage());
                ServiceMain.log.trace(e.getMessage(), e);
                continue;
            }
        }
    }

    /**
     * @param managerClass
     * @param service
     * @return
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    private static ConnectionHandlerManager instantiateManagerWithService(
            final Class<? extends ConnectionHandlerManager> managerClass,
            final Service<?> service) {
        // It should have a constructor with service as argument
        for (final Constructor<?> c : managerClass.getConstructors()) {
            if ((c.getParameterCount() == 1) && Service.class.isAssignableFrom(c.getParameterTypes()[0])) {
                try {
                    return (ConnectionHandlerManager) c.newInstance(service);
                } catch (final Exception e) {
                    // Try next constructor if it exists...
                    ServiceMain.log.warn("Exception while creating instance of {}: {}", managerClass, e.getMessage());
                    ServiceMain.log.trace(e.getMessage(), e);
                }
            }
        }

        // Try the empty constructor if it fails
        ServiceMain.log.debug("Attempting fallback empty constructor for {}", managerClass);
        try {
            return managerClass.newInstance();
        } catch (final Exception e) {
            ServiceMain.log.warn("Unable to instantiate manager type {} of service {}: {}",
                    managerClass,
                    service,
                    e.getMessage());
            ServiceMain.log.trace(e.getMessage(), e);
            return null;
        }
    }

    public static <T> void main(final String[] args) {
        try (
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(ServiceMain.class.getResourceAsStream("/service-version")))) {
            String line;
            while ((line = br.readLine()) != null) {
                ServiceMain.log.info(line);
            }
        } catch (final IOException e) {
            ServiceMain.log.info("Unable to detect service version: {}", e.getMessage());
        }

        final String servicePackage = System.getenv("SERVICE_PACKAGE");
        if ((servicePackage == null) || servicePackage.isEmpty()) {
            ServiceMain.reflections = new Reflections();
        } else {
            ServiceMain.reflections = new Reflections(servicePackage);
        }

        // Get service from package

        @SuppressWarnings("resource")
        final ServiceManager<T> manager = new ServiceManager<>();

        // The following can run in the user thread, it will call the constructor, which is required anyway, and only
        // when that succeeds, starts the manager.
        ServiceMain.serviceExecutor.submit(() -> {
            try {
                @SuppressWarnings("unchecked")
                final Class<? extends Service<T>> serviceClass = (Class<? extends Service<T>>) ServiceMain
                        .getServiceClass();

                ServiceMain.log.debug("Found {} as service type", serviceClass);
                final Service<T> service = serviceClass.newInstance();

                ServiceMain.log.info("Starting service {}", service);
                ServiceMain.registerMessageHandlers(service);
                manager.start(service);
            } catch (final Exception e) {
                // Catch any exception, would be nice to let the manager forward it to the orchestrator when available
                ServiceMain.log.error("Error while starting service: {}", e.getMessage());
                ServiceMain.log.trace(e.getMessage(), e);
                manager.close();
            }
        });

        manager.join();
    }
}
