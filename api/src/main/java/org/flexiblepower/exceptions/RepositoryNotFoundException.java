/**
 * File RepositoryNotFoundException.java
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
 * RepositoryNotFoundException
 *
 * @version 0.1
 * @since Apr 13, 2017
 */
public class RepositoryNotFoundException extends NotFoundException {

    /**
     *
     */
    private static final long serialVersionUID = -2287421836765202024L;

    public static final String REPOSITORY_NOT_FOUND_MESSAGE = "Repository not found";

    public RepositoryNotFoundException(final String msg, final Exception cause) {
        super(msg, cause);
    }

    /**
     * @param repositoryName
     */
    public RepositoryNotFoundException(final String repositoryName) {
        super(RepositoryNotFoundException.REPOSITORY_NOT_FOUND_MESSAGE + ": " + repositoryName.toString());
    }

    /**
     * @param repositoryName
     */
    public RepositoryNotFoundException() {
        super(RepositoryNotFoundException.REPOSITORY_NOT_FOUND_MESSAGE);
    }

}
