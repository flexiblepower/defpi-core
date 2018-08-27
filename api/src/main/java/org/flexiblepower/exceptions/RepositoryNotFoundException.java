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
 * RepositoryNotFoundException
 *
 * @version 0.1
 * @since Apr 13, 2017
 */
public class RepositoryNotFoundException extends NotFoundException {

    private static final long serialVersionUID = -2287421836765202024L;

    /**
     * The message string stating that the repository is not found
     */
    public static final String REPOSITORY_NOT_FOUND_MESSAGE = "Repository not found";

    /**
     * Create a RepositoryNotFoundException with the given message
     *
     * @param msg The message to show with the exception
     * @param cause The underlying Throwable that caused this exception
     */
    public RepositoryNotFoundException(final String msg, final Exception cause) {
        super(msg, cause);
    }

    /**
     * Create a RepositoryNotFoundException that the repository with the provided name could not be found
     *
     * @param repositoryName the name of the repository that was not found
     */
    public RepositoryNotFoundException(final String repositoryName) {
        super(RepositoryNotFoundException.REPOSITORY_NOT_FOUND_MESSAGE + ": " + repositoryName.toString());
    }

}
