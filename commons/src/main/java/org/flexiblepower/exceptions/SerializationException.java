/*-
 * #%L
 * dEF-Pi commons Library
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

/**
 * SerializationException
 *
 * @version 0.1
 * @since May 12, 2017
 */
public class SerializationException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = -6842988453232951453L;

    /**
     * @param cause The underlying cause of the serialization exception
     */
    public SerializationException(final Throwable cause) {
        super("Error serializing message", cause);
    }

    /**
     * @param msg The message to add to the exception
     */
    public SerializationException(final String msg) {
        super(msg);
    }

}
