/**
 * File RepositoryNotFoundException.java
 *
 * Copyright 2017 FAN
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
