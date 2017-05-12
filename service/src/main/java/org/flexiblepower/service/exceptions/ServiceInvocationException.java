/**
 * File ServiceInvocationException.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service.exceptions;

/**
 * ServiceInvocationException
 *
 * @author coenvl
 * @version 0.1
 * @since May 10, 2017
 */
public class ServiceInvocationException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = -1747713538435507438L;

    public ServiceInvocationException(final String msg) {
        super(msg);
    }

    /**
     * @param string
     * @param e
     */
    public ServiceInvocationException(final String msg, final Throwable cause) {
        super(msg, cause);
    }

}
