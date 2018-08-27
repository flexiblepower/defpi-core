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
 * UserNotFoundException
 *
 * @version 0.1
 * @since Apr 13, 2017
 */
public class UserNotFoundException extends NotFoundException {

    private static final long serialVersionUID = -6841254000987456321L;

    /**
     * The message string stating that the nodepool is not found
     */
    public static final String USER_NOT_FOUND_MESSAGE = "User not found";

    /**
     * Create an exception with the default message that the user ws not found
     */
    public UserNotFoundException() {
        super(UserNotFoundException.USER_NOT_FOUND_MESSAGE);
    }

    /**
     * Create an exception with the default message that the user was not found with that name
     *
     * @param userName the name of the user that could not be found
     */
    public UserNotFoundException(final String userName) {
        super(UserNotFoundException.USER_NOT_FOUND_MESSAGE + " with name " + userName);
    }

}
