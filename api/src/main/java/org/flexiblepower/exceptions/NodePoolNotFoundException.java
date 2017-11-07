/**
 * File ServiceNotFoundException.java
 *
 * Copyright 2017 FAN
 */
package org.flexiblepower.exceptions;

import java.net.URI;

/**
 * ServiceNotFoundException
 *
 * @version 0.1
 * @since Apr 13, 2017
 */
public class NodePoolNotFoundException extends NotFoundException {

    /**
     *
     */
    private static final long serialVersionUID = -7761214427627076445L;

    public static final String SERVICE_NOT_FOUND_MESSAGE = "NodePool not found";

    public NodePoolNotFoundException() {
        super(NodePoolNotFoundException.SERVICE_NOT_FOUND_MESSAGE);
    }

    public NodePoolNotFoundException(final URI uri) {
        super(NodePoolNotFoundException.SERVICE_NOT_FOUND_MESSAGE + ": " + uri.toString());
    }

}
