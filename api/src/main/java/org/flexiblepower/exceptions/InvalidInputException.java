/**
 * File InvalidInputException.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.exceptions;

/**
 * InvalidInputException
 *
 * @author coenvl
 * @version 0.1
 * @since Apr 12, 2017
 */
public class InvalidInputException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = -4533673102194251388L;

    /**
     * @param invalidObjectIdMessage
     */
    public InvalidInputException(final String message) {
        super(message);
    }

}
