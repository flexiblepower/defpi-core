/*-
 * #%L
 * dEF-Pi service managing library
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
package org.flexiblepower.service.exceptions;

/**
 * ServiceInvocationException
 *
 * @version 0.1
 * @since May 10, 2017
 */
public class ServiceInvocationException extends Exception {

    private static final long serialVersionUID = -1747713538435507438L;

    /**
     * Create a ServiceInvocationException with a custom message
     *
     * @param msg the message to add to the exception {@link Exception#getMessage()}
     */
    public ServiceInvocationException(final String msg) {
        super(msg);
    }

    /**
     * Create a ServiceInvocationException with a custom message
     *
     * @param msg the message to add to the exception {@link Exception#getMessage()}
     * @param cause The underlyting cause Trowable {@link Exception#getCause()}
     */
    public ServiceInvocationException(final String msg, final Throwable cause) {
        super(msg, cause);
    }

}
