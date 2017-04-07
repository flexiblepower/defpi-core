/**
 * File InvalidObjectIdException.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.exceptions;

/**
 * InvalidObjectIdException
 *
 * @author coenvl
 * @version 0.1
 * @since Apr 6, 2017
 */
public class InvalidObjectIdException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 5363064629938233350L;

    private static final String DEFAULT_MESSAGE = "The provided id is not a valid ObjectId";

    public InvalidObjectIdException() {
        super(InvalidObjectIdException.DEFAULT_MESSAGE);
    }

    public InvalidObjectIdException(final String id) {
        super(InvalidObjectIdException.DEFAULT_MESSAGE + " (" + id + ")");
    }

}
