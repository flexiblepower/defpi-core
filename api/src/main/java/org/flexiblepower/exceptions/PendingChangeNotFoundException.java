/**
 * File ServiceNotFoundException.java
 *
 * Copyright 2017 FAN
 */
package org.flexiblepower.exceptions;

/**
 * ServiceNotFoundException
 *
 * @version 0.1
 * @since Apr 13, 2017
 */
public class PendingChangeNotFoundException extends NotFoundException {

    /**
     *
     */
    private static final long serialVersionUID = -7761214427667076445L;

    public static final String SERVICE_NOT_FOUND_MESSAGE = "PendingChange not found";

    public PendingChangeNotFoundException() {
        super(PendingChangeNotFoundException.SERVICE_NOT_FOUND_MESSAGE);
    }

}
