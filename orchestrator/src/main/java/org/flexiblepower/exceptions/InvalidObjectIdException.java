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

    public static final String INVALID_OBJECT_ID_MESSAGE = "The provided id is not a valid ObjectId";

    public InvalidObjectIdException() {
        super(InvalidObjectIdException.INVALID_OBJECT_ID_MESSAGE);
    }

    public InvalidObjectIdException(final String id) {
        super(InvalidObjectIdException.INVALID_OBJECT_ID_MESSAGE + " (" + id + ")");
    }

}
