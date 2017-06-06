/**
 * File ConnectionException.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.exceptions;

/**
 * ConnectionException
 *
 * @author coenvl
 * @version 0.1
 * @since Jun 6, 2017
 */
public class ConnectionException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = -2817239129713197126L;

    public ConnectionException(final String msg) {
        super(msg);
    }

}
