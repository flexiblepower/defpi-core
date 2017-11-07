/**
 * File ProcessNotFoundException.java
 *
 * Copyright 2017 FAN
 */
package org.flexiblepower.exceptions;

/**
 * ProcessNotFoundException
 *
 * @version 0.1
 * @since Apr 12, 2017
 */
public class ProcessNotFoundException extends NotFoundException {

    /**
     *
     */
    private static final long serialVersionUID = -7947643331231772808L;

    /**
     * @param msg
     */
    public ProcessNotFoundException(final String processId) {
        super("Could not find Process with id " + processId);
    }

}
