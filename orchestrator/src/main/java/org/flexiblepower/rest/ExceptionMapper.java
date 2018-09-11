/*-
 * #%L
 * dEF-Pi REST Orchestrator
 * %%
 * Copyright (C) 2017 - 2018 Flexible Power Alliance Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
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
