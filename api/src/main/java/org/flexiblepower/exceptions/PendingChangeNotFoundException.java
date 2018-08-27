/*
 * File PendingChangeNotFoundException.java
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

import org.bson.types.ObjectId;

/**
 * ServiceNotFoundException
 *
 * @version 0.1
 * @since Apr 13, 2017
 */
public class PendingChangeNotFoundException extends NotFoundException {

    private static final long serialVersionUID = -7761214427667076445L;

    /**
     * The message string stating that the pending change is not found
     */
    public static final String PENDING_CHANGE_NOT_FOUND = "PendingChange not found";

    /**
     * Create an exception with the default message that the pendinc change is not found
     *
     * @param id the ObjectId of the pending change that could not be found
     */
    public PendingChangeNotFoundException(final ObjectId id) {
        super(PendingChangeNotFoundException.PENDING_CHANGE_NOT_FOUND);
    }

}
