/**
 * File OrchestrationException.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.exceptions;

/**
 * OrchestrationException
 *
 * @author coenvl
 * @version 0.1
 * @since May 1, 2017
 */
public class OrchestrationException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 2046270623239734474L;

    /**
     *
     */
    public OrchestrationException(final String msg) {
        super(msg);
    }

    public OrchestrationException(final Throwable t) {
        super(t);
    }

    public OrchestrationException(final String msg, final Throwable t) {
        super(msg, t);
    }

}
