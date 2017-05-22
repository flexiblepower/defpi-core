/**
 * File ConnectionModificationException.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service.exceptions;

/**
 * ConnectionModificationException
 *
 * @author coenvl
 * @version 0.1
 * @since May 10, 2017
 */
public class ConnectionModificationException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 958964594712473423L;

    public ConnectionModificationException(final String msg) {
        super(msg);
    }

}