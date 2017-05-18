/**
 * File ServiceMain.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

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
public final class ServiceMain {

    private static final Logger log = LoggerFactory.getLogger(ServiceMain.class);

    /**
     * @throws ServiceInvocationException
     *
     */
    public ServiceMain() throws ServiceInvocationException {
        // Get service from package
        final Service service = ServiceMain.getService();
        ServiceMain.log.info("Started service {}", service);

        @SuppressWarnings("unused")
        final ServiceManager manager = new ServiceManager(service);
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
     * @return
     * @throws ServiceInvocationException
     */
    // private static Set<MessageHandler> getMessageHandlers() throws ServiceInvocationException {
    // final Reflections reflections = new Reflections("org.flexiblepower");
    // final Set<Class<? extends MessageHandler>> set = reflections.getSubTypesOf(MessageHandler.class);
    //
    // // Remove our abstract implementation which is not meant to be instantiated, but not necessarily used
    // set.remove(AbstractMessageHandler.class);
    //
    // if (set.isEmpty()) {
    // ServiceMain.log
    // .warn("No message handlers have been found, service will not be able to respond to messages");
    // } else {
    // ServiceMain.log.info("Found {} message handlers: {}", set.size(), set);
    // }
    //
    // final Set<MessageHandler> ret = new HashSet<>();
    // for (final Class<? extends MessageHandler> handlerClass : set) {
    // try {
    // ret.add(handlerClass.newInstance());
    // } catch (InstantiationException | IllegalAccessException e) {
    // throw new ServiceInvocationException("Unable to start service of type " + handlerClass, e);
    // }
    // }
    //
    // return ret;
    // }

    @SuppressWarnings("unused")
    public static void main(final String[] args) throws ServiceInvocationException {
        // Launch new service
        new ServiceMain();
    }

}
