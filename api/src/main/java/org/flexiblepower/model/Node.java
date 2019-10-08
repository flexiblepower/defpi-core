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

import java.util.Date;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A Node is a piece of hardware in the dEF-Pi environment capable of running processes.
 *
 * @version 0.1
 * @since 20 mrt. 2017
 * @see UnidentifiedNode
 * @see PublicNode
 * @see PrivateNode
 */
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Must be generated for Morphia
@EqualsAndHashCode(exclude = {"lastSync", "status"})
public abstract class Node {

    /**
     * DockerNodeStatus indicates what the status of a node is in the docker swarm.
     *
     * @version 0.1
     * @since 20 mrt. 2017
     */
    public enum DockerNodeStatus {
        /**
         * The other node could not be found
         */
        MISSING,
        /**
         * Unable to determine the node status
         */
        UNKNOWN,
        /**
         * The node is down, i.e. not currently accepting processes
         */
        DOWN,
        /**
         * The node is up and ready to run processes
         */
        READY,
        /**
         * The node is not connected to the swarm, i.e. has switched off or left the swarm
         */
        DISCONNECTED;

        /**
         * Build the DockerNodeStatus enum from a piece of text. Useful for during deserialization
         * 
         * @param text the textual representation of the node status
         * @return The enum that represents the text argument
         */
        public static DockerNodeStatus fromString(final String text) {
            for (final DockerNodeStatus s : DockerNodeStatus.values()) {
                if (s.toString().equalsIgnoreCase(text)) {
                    return s;
                }
            }
            return UNKNOWN;
        }
    }

    /**
     * 
     */
    @Id
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    protected ObjectId id;

    /**
     * The name is only for human recognition of the node
     */
    @Setter
    protected String name;
    
    /**
     * 
     */
    protected String dockerId;

    /**
     * The hostname of the node machine
     */
    @Setter
    protected String hostname;

    /**
     * The last time the status was retrieved from docker
     */
    @Setter
    protected Date lastSync;

    /**
     * The reported status of the node
     */
    @Setter
    protected DockerNodeStatus status;

    /**
     * 
     */
    @Setter
    protected Architecture architecture;

    /**
     * Create a node representation with the provided arguments
     * 
     * @param dockerId The docker id of the node
     * @param hostname The IP address or hostname of the node
     * @param architecture The architecture of the node
     */
    public Node(final String dockerId, final String hostname, final Architecture architecture) {
        this.dockerId = dockerId;
        this.hostname = hostname;
        this.status = DockerNodeStatus.UNKNOWN;
        this.architecture = architecture;
        this.lastSync = new Date(); // now
    }

}
