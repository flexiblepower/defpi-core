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
 * NodePoolNotFoundException
 *
 * @version 0.1
 * @since Apr 13, 2017
 */
public class NodePoolNotFoundException extends NotFoundException {

    private static final long serialVersionUID = -7761214427627076445L;

    /**
     * The message string stating that the nodepool is not found
     */
    public static final String NODEPOOL_NOT_FOUND_MESSAGE = "NodePool not found";

    /**
     * Create an exception with the default message that the nodepool is not found
     */
    public NodePoolNotFoundException() {
        super(NodePoolNotFoundException.NODEPOOL_NOT_FOUND_MESSAGE);
    }

}
