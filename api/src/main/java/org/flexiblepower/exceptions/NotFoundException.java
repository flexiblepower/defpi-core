/**
 * File NotFoundException.java
 *
 * Copyright 2017 FAN
 */
package org.flexiblepower.exceptions;

/**
 * NotFoundException
 *
 * @version 0.1
 * @since Apr 12, 2017
 */
public abstract class NotFoundException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = -6337738231851288131L;

    protected NotFoundException(final String msg) {
        super(msg);
    }

    protected NotFoundException(final String msg, final Throwable cause) {
        super(msg, cause);
    }

}
