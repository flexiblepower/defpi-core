/*
 * File ServiceNotFoundException.java
 *
 * Copyright 2017 FAN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flexiblepower.exceptions;

/**
 * ServiceNotFoundException
 *
 * @version 0.1
 * @since Apr 13, 2017
 */
public class ServiceNotFoundException extends NotFoundException {

    private static final long serialVersionUID = -7761214427127076445L;

    /**
     * The message string stating that the repository is not found
     */
    public static final String SERVICE_NOT_FOUND_MESSAGE = "Service not found";

    /**
     * Create an exception with the default message that the service could not be found
     */
    public ServiceNotFoundException() {
        super(ServiceNotFoundException.SERVICE_NOT_FOUND_MESSAGE);
    }

    /**
     * Create a ServiceNotFoundException that the service with the provided id could not be found
     *
     * @param id the ID of the service that was not found
     */
    public ServiceNotFoundException(final String id) {
        super(ServiceNotFoundException.SERVICE_NOT_FOUND_MESSAGE + ": " + id);
    }

    /**
     * Create a ServiceNotFoundException with an underlying cause
     *
     * @param cause the underlying cause why the service could not be found
     */
    public ServiceNotFoundException(final Throwable cause) {
        super(ServiceNotFoundException.SERVICE_NOT_FOUND_MESSAGE + ": " + cause.getMessage());
    }

}
