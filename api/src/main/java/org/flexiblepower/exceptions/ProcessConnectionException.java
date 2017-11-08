/**
 * File ProcessConnectionException.java
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
 * ProcessConnectionException
 *
 * @version 0.1
 * @since Aug 7, 2017
 */
public class ProcessConnectionException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 6925736740936431592L;

    /**
     *
     */
    public ProcessConnectionException(final Throwable t) {
        super(t);
    }

}
