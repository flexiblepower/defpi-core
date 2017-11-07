/**
 * File ServiceMain.java
 *
 * Copyright 2017 FAN
 */
package org.flexiblepower.service;

import java.lang.reflect.Constructor;
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
        final Set<Class<? extends ConnectionHandler>> set = ServiceMain.reflections
                .getSubTypesOf(ConnectionHandler.class);

        if (set.isEmpty()) {
            ServiceMain.log
                    .warn("No connection handlers have been found, service will not be able to respond to messages");
        }

        ServiceMain.log.info("Found {} message handlers: {}", set.size(), set);
        for (final Class<? extends ConnectionHandler> handlerClass : set) {
            if (!handlerClass.isInterface()) {
                // Only look for implementations
                continue;
            }

            try {
                final InterfaceInfo info = handlerClass.getAnnotation(InterfaceInfo.class);
                if (info == null) {
                    ServiceMain.log.warn("Missing @InterfaceInfo annotation on {}, skipping", handlerClass);
                    continue;
                }

                final Class<? extends ConnectionHandlerManager> managerClass = info.manager();

                ConnectionHandlerManager manager = null;
                // It should have a constructor with service as argument
                for (final Constructor<?> c : managerClass.getConstructors()) {
                    if ((c.getParameterCount() == 1) && Service.class.isAssignableFrom(c.getParameterTypes()[0])) {
                        try {
                            manager = (ConnectionHandlerManager) c.newInstance(service);
                            break;
                        } catch (final Exception e) {
                            // Try next constructor maybe?
                            ServiceMain.log
                                    .warn("Exception while creating instance of {}: {}", managerClass, e.getMessage());
                            ServiceMain.log.trace(e.getMessage(), e);
                        }
                    }
                }

                if (manager == null) {
                    // Try the empty constructor if it fails
                    ServiceMain.log.debug("Attempting fallback empty constructor for {}", managerClass);
                    manager = managerClass.newInstance();
                }

                ConnectionManager.registerConnectionHandlerFactory(handlerClass, manager);

            } catch (InstantiationException | IllegalAccessException e) {
                // Try and continue with the next interface
                ServiceMain.log.warn("Unable to instantiate manager for type {} of service {}: {}",
                        handlerClass,
                        service,
                        e.getMessage());
                ServiceMain.log.trace(e.getMessage(), e);
                continue;
            }
        }
    }

    public static <T> void main(final String[] args) {
        ServiceMain.reflections = new Reflections();
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

                ServiceMain.log.debug("Found class {} as service type", serviceClass);
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
