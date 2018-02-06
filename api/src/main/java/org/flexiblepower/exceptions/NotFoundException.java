/**
 * File NotFoundException.java
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
 * NotFoundException 404
 *
 * @version 0.1
 * @since Apr 12, 2017
 */
public abstract class NotFoundException extends Exception {

    private static final long serialVersionUID = -6337738231851288131L;

    /**
     * Create a NotFoundException with the given message
     *
     * @param msg The message to show with the exception
     */
    protected NotFoundException(final String msg) {
        super(msg);
    }

    /**
     * Create a NotFoundException with the given message
     *
     * @param msg The message to show with the exception
     * @param cause The underlying Throwable that caused this exception
     */
    protected NotFoundException(final String msg, final Throwable cause) {
        super(msg, cause);
    }

}
