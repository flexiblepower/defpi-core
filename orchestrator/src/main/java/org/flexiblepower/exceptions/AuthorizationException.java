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

    public final static String UNAUTHORIZED_MESSAGE = "The user is not authorized to perform this operation";

    public AuthorizationException() {
        super(AuthorizationException.UNAUTHORIZED_MESSAGE);
    }

}
