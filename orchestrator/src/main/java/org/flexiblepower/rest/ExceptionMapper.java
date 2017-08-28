/**
 * File ExceptionMapper.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.rest;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.flexiblepower.exceptions.ApiException;
import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.InvalidInputException;
import org.flexiblepower.exceptions.NotFoundException;

import lombok.extern.slf4j.Slf4j;

/**
 * ExceptionMapper
 *
 * @author coenvl
 * @version 0.1
 * @since Apr 3, 2017
 */
@Slf4j
public class ExceptionMapper implements javax.ws.rs.ext.ExceptionMapper<Exception> {

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.ext.ExceptionMapper#toResponse(java.lang.Throwable)
     */
    @Override
    public Response toResponse(final Exception exception) {
        if (WebApplicationException.class.isAssignableFrom(exception.getClass())) {
            return ((WebApplicationException) exception).getResponse();
        } else if (AuthorizationException.class.isAssignableFrom(exception.getClass())) {
            ExceptionMapper.log.warn("Unauthorized call to API: {}", exception.getMessage());
            ExceptionMapper.log.trace(exception.getMessage(), exception);
            return Response.status(Status.UNAUTHORIZED)
                    .entity(ApiException.createErrorPage("Unauthorized", exception.getMessage(), null))
                    .type(MediaType.TEXT_HTML)
                    .build();
        } else if (InvalidInputException.class.isAssignableFrom(exception.getClass())) {
            ExceptionMapper.log.warn("Invalid input provided: {} ", exception.getMessage());
            ExceptionMapper.log.trace(exception.getMessage(), exception);
            return Response.status(Status.BAD_REQUEST)
                    .entity(ApiException.createErrorPage("Invalid input", exception.getMessage(), null))
                    .type(MediaType.TEXT_HTML)
                    .build();
        } else if (NotFoundException.class.isAssignableFrom(exception.getClass())) {
            ExceptionMapper.log.warn("Object not found: {}", exception.getMessage());
            ExceptionMapper.log.trace(exception.getMessage(), exception);
            return Response.status(Status.NOT_FOUND)
                    .entity(ApiException.createErrorPage("Resource not found", exception.getMessage(), null))
                    .type(MediaType.TEXT_HTML)
                    .build();
        } else {
            ExceptionMapper.log.error("Unexpected exception: {}", exception.getMessage());
            ExceptionMapper.log.trace(exception.getMessage(), exception);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiException.createErrorPage("Unexpected exception occurred",
                            "Exception message: " + exception.getMessage(),
                            exception))
                    .type(MediaType.TEXT_HTML)
                    .build();
        }
    }

}
