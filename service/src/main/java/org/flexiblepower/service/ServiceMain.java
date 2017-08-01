/**
 * File ServiceMain.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.lang.reflect.Constructor;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    private static final long SERVICE_CONSTRUCTOR_TIMEOUT_SECONDS = 30;
    private static Service service;
    private static Exception serviceConstructorException;
    private static Reflections reflections;

    public static Service createInstance(final ExecutorService executor) throws ServiceInvocationException {
        ServiceMain.reflections = new Reflections(); // TODO: this could be nicer
        // Get service from package
        ServiceMain.service = ServiceMain.getService(executor);
        if (ServiceMain.service == null) {
            throw new ServiceInvocationException(ServiceMain.serviceConstructorException.getMessage());
        }
        ServiceMain.registerMessageHandlers();
        ServiceMain.log.info("Started service {}", ServiceMain.service);
        return ServiceMain.service;
    }

    /**
     * @return
     * @throws ServiceInvocationException
     */
    private static Service getService(final ExecutorService executor) throws ServiceInvocationException {
        final Set<Class<? extends Service>> set = ServiceMain.reflections.getSubTypesOf(Service.class);

        // Must have exactly 1 result
        if (set.size() > 1) {
            throw new ServiceInvocationException(
                    "Unable to start service, more than 1 service implementations found: " + set.toString());
        } else if (set.isEmpty()) {
            throw new ServiceInvocationException("Unable to start service, no service implementations found");
        }

        final Class<? extends Service> serviceClass = set.iterator().next();
        ServiceMain.log.debug("Found class {} as service type", serviceClass);

        // Try to create a service with the default constructor
        final CallableService cs = new CallableService(serviceClass);
        final Future<Service> future = executor.submit(cs);
        Service serviceInstance = null;
        try {
            serviceInstance = future.get(ServiceMain.SERVICE_CONSTRUCTOR_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            ServiceMain.serviceConstructorException = e;
        }
        return serviceInstance;
    }

    /**
     * Registers all connectionHandlerFactories for their corresponding connectionHandlers.
     *
     * @param service
     */
    private static void registerMessageHandlers() {
        final Set<Class<? extends ConnectionHandler>> set = ServiceMain.reflections
                .getSubTypesOf(ConnectionHandler.class);

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
                    ServiceMain.log.debug("Missing @InterfaceInfo annotation on {}, skipping", handlerClass);
                    continue;
                }

                final Class<? extends ConnectionHandlerFactory> factoryClass = info.factory();

                ConnectionHandlerFactory chf = null;
                // It should have a constructor with service as argument
                for (final Constructor<?> c : factoryClass.getConstructors()) {
                    if ((c.getParameterCount() == 1) && Service.class.isAssignableFrom(c.getParameterTypes()[0])) {
                        try {
                            chf = (ConnectionHandlerFactory) c.newInstance(ServiceMain.service);
                            break;
                        } catch (final Exception e) {
                            // Do nothing try next...
                        }
                    }
                }
                if (chf == null) {
                    // Try the empty constructor if it fails
                    chf = factoryClass.newInstance();
                }
                ConnectionManager.registerConnectionHandlerFactory(handlerClass, chf);

            } catch (InstantiationException | IllegalAccessException e) {
                ServiceMain.log.warn("Unable to instantiate factory for type {} for service ",
                        handlerClass,
                        ServiceMain.service);
                ServiceMain.log.trace("Unable to instantiate factory", e);

                continue;
            }
        }
    }
}

class CallableService implements Callable<Service> {

    private final Class<? extends Service> serviceClass;

    @Override
    public Service call() throws Exception {
        Service serviceInstance = null;
        try {
            serviceInstance = this.serviceClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ServiceInvocationException("Unable to start service of type " + this.serviceClass, e);
        }
        return serviceInstance;
    }

    public CallableService(final Class<? extends Service> serviceClass) {
        this.serviceClass = serviceClass;
    }
}
