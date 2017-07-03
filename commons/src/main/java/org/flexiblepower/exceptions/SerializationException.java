/**
 * File SerializationException.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.exceptions;

/**
 * SerializationException
 *
 * @author coenvl
 * @version 0.1
 * @since May 12, 2017
 */
public class SerializationException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = -6842988453232951453L;

    /**
     * @param e
     */
    public SerializationException(final Throwable cause) {
        super("Error serializing message", cause);
    }

    /**
     * @param string
     */
    public SerializationException(final String msg) {
        super(msg);
    }

}
