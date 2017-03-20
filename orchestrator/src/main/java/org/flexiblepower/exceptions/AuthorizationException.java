/**
 * File UnauthorizedException.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.exceptions;

/**
 * UnauthorizedException
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 20 mrt. 2017
 */
public class AuthorizationException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 6350891352054597536L;

    private static final String DEFAULT_MESSAGE = "The current user is not allowed to use this functionality";

    public AuthorizationException() {
        super(AuthorizationException.DEFAULT_MESSAGE);
    }

}
