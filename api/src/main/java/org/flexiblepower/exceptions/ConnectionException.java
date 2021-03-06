/*-
 * #%L
 * dEF-Pi API
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
package org.flexiblepower.exceptions;

import javax.ws.rs.core.Response.Status;

/**
 * OrchestrationException
 *
 * @version 0.1
 * @since May 1, 2017
 */
public class ConnectionException extends ApiException {

    private static final long serialVersionUID = -7196576043993506083L;

    /**
     * Create a connection exception with the provided error message
     *
     * @param msg The message to add to the exception
     */
    public ConnectionException(final String msg) {
        super(Status.CONFLICT, msg);
    }

    /**
     * Create a connection exception that was cause by an underlying case throwable
     *
     * @param t the thrown cause of this exception
     */
    public ConnectionException(final Throwable t) {
        super(Status.CONFLICT, t);
    }

}
