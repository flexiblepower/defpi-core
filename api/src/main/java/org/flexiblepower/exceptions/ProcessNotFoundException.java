/**
 * File ProcessNotFoundException.java
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
 * ProcessNotFoundException
 *
 * @version 0.1
 * @since Apr 12, 2017
 */
public class ProcessNotFoundException extends NotFoundException {

    private static final long serialVersionUID = -7947643331231772808L;

    /**
     * @param processId Create an execption that the process with the provided ID cannot be found.
     */
    public ProcessNotFoundException(final ObjectId processId) {
        super("Could not find Process with id " + processId.toString());
    }

}
