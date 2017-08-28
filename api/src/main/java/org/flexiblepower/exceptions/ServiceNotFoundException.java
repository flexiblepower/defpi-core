/**
 * File ServiceNotFoundException.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.exceptions;

import java.net.URI;

/**
 * ServiceNotFoundException
 *
 * @author coenvl
 * @version 0.1
 * @since Apr 13, 2017
 */
public class ServiceNotFoundException extends NotFoundException {

    /**
     *
     */
    private static final long serialVersionUID = -7761214427127076445L;

    public static final String SERVICE_NOT_FOUND_MESSAGE = "Service not found";

    public ServiceNotFoundException() {
        super(ServiceNotFoundException.SERVICE_NOT_FOUND_MESSAGE);
    }

    public ServiceNotFoundException(final URI uri) {
        super(ServiceNotFoundException.SERVICE_NOT_FOUND_MESSAGE + ": " + uri.toString());
    }

    public ServiceNotFoundException(final Throwable cause) {
        super(ServiceNotFoundException.SERVICE_NOT_FOUND_MESSAGE + ": " + cause.getMessage());
    }

}
