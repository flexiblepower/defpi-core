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

/**
 * InvalidObjectIdException
 *
 * @version 0.1
 * @since Apr 6, 2017
 */
public class InvalidObjectIdException extends InvalidInputException {

    private static final long serialVersionUID = 5363064629938233350L;

    /**
     * The default exception message
     */
    public static final String INVALID_OBJECT_ID_MESSAGE = "The provided id is not a valid ObjectId";

    /**
     * Create an exception stating that the provided id is not a valid ObjectId, and show the invalid input
     *
     * @param id The string that was not a valid ObjectId
     */
    public InvalidObjectIdException(final String id) {
        super(InvalidObjectIdException.INVALID_OBJECT_ID_MESSAGE + " (" + id + ")");
    }

}
