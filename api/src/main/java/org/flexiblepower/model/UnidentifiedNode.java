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
package org.flexiblepower.model;

import org.mongodb.morphia.annotations.Entity;

import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * A UnidentifiedNode is a node that is neither public or private. This means it is available in the swarm, but as long
 * as it is not assigned public or private, no process can be run on it.
 *
 * @version 0.1
 * @since 20 mrt. 2017
 * @see PublicNode
 * @see PrivateNode
 */
@Entity
@ToString
@NoArgsConstructor
public class UnidentifiedNode extends Node {

    /**
     * Create an unidentified node representation with the provided arguments
     *
     * @param dockerId The docker id of the node
     * @param hostname The IP address or hostname of the node
     * @param architecture The architecture of the node
     */
    public UnidentifiedNode(final String dockerId, final String hostname, final Architecture architecture) {
        super(dockerId, hostname, architecture);
    }

}
