/**
 * File OrchestrationException.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.exceptions;

import javax.ws.rs.core.Response.Status;

/**
 * OrchestrationException
 *
 * @author coenvl
 * @version 0.1
 * @since May 1, 2017
 */
public class ConnectionException extends ApiException {

    /**
     *
     */
    private static final long serialVersionUID = -7196576043993506083L;

    /**
     *
     */
    public ConnectionException(final String msg) {
        super(Status.CONFLICT, msg);
    }

    public ConnectionException(final Throwable t) {
        super(Status.CONFLICT, t);
    }

}
