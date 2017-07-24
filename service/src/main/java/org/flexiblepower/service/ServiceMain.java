/**
 * File ServiceMain.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import org.flexiblepower.service.exceptions.ServiceInvocationException;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ServiceMain
 *
 * @author coenvl
 * @version 0.1
 * @since May 10, 2017
 */
/**
 * ServiceMain
 *
 * @author coenvl
 * @version 0.1
 * @since Jul 24, 2017
 */
public final class ServiceMain {

    private static final Logger log = LoggerFactory.getLogger(ServiceMain.class);

    /**
     * @throws ServiceInvocationException
     *
     */
    public ServiceMain() throws ServiceInvocationException {
        // Get service from package
        final Service service = ServiceMain.getService();
        ServiceMain.registerMessageHandlers(service);
        ServiceMain.log.info("Started service {}", service);

        final ServiceManager manager = new ServiceManager(service);
        manager.join();
    }

    /**
     * @return
     * @throws ServiceInvocationException
     */
    private static Service getService() throws ServiceInvocationException {
        final Reflections reflections = new Reflections("org.flexiblepower");
        final Set<Class<? extends Service>> set = reflections.getSubTypesOf(Service.class);

        // Must have exactly 1 result
        if (set.size() > 1) {
            throw new ServiceInvocationException(
                    "Unable to start service, more than 1 service implementations found: " + set.toString());
        } else if (set.isEmpty()) {
            throw new ServiceInvocationException("Unable to start service, no service implementations found");
        }

        final Class<? extends Service> serviceClass = set.iterator().next();
        ServiceMain.log.debug("Found class {} as service type", serviceClass);

        try {
            // Try to create a service with the default constructor
            return serviceClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ServiceInvocationException("Unable to start service of type " + serviceClass, e);
        }
    }

    /**
     * Registers all connectionHandlerFactories for their corresponding connectionHandlers.
     *
     * @param service
     */
    private static void registerMessageHandlers(final Service service) {
        final Reflections reflections = new Reflections("org.flexiblepower");
        final Set<Class<? extends ConnectionHandler>> set = reflections.getSubTypesOf(ConnectionHandler.class);

        if (set.isEmpty()) {
            ServiceMain.log
                    .warn("No connection handlers have been found, service will not be able to respond to messages");
        } else {
            ServiceMain.log.info("Found {} message handlers: {}", set.size(), set);
        }

        for (final Class<? extends ConnectionHandler> handlerClass : set) {
            try {
                final InterfaceInfo info = handlerClass.getAnnotation(InterfaceInfo.class);
                if (info == null) {
                    ServiceMain.log.warn("Missiong @InterfaceInfo annotation on {}", handlerClass);
                    continue;
                }

                final Class<? extends ConnectionHandlerFactory> factoryClass = info.factory();

                ConnectionHandlerFactory chf = null;
                try {
                    // It should have a constructor with service as argument
                    chf = factoryClass.getConstructor(Service.class).newInstance(service);
                } catch (final NoSuchMethodException e) {
                    // Try the empty constructor if it fails
                    chf = factoryClass.newInstance();
                }
                ConnectionManager.registerConnectionHandlerFactory(handlerClass, chf);

            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                ServiceMain.log.warn("Unable to instantiate factory for type {} for service ", handlerClass, service);
                ServiceMain.log.trace("Unable to instantiate factory", e);

                continue;
            }
        }
    }

    @SuppressWarnings("unused")
    public static void main(final String[] args) throws ServiceInvocationException {
        // Launch new service
        new ServiceMain();
    }

}
