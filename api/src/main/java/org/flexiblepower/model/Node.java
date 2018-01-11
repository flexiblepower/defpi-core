/**
 * File Node.java
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
 * Node
 *
 * @version 0.1
 * @since 20 mrt. 2017
 */
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(exclude = {"lastSync", "status"})
public abstract class Node {

    public static enum DockerNodeStatus {
        MISSING,
        UNKNOWN,
        DOWN,
        READY,
        DISCONNECTED;

        public static DockerNodeStatus fromString(final String text) {
            for (final DockerNodeStatus s : DockerNodeStatus.values()) {
                if (s.toString().equalsIgnoreCase(text)) {
                    return s;
                }
            }
            return UNKNOWN;
        }
    }

    @Id
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    protected ObjectId id;

    protected String dockerId;

    @Setter
    protected String hostname;

    /**
     * The last time the status was retrieved from docker
     */
    @Setter
    protected Date lastSync;

    @Setter
    protected DockerNodeStatus status;

    @Setter
    protected Architecture architecture;

    public Node(final String dockerId, final String hostname, final Architecture architecture) {
        this.dockerId = dockerId;
        this.hostname = hostname;
        this.status = DockerNodeStatus.UNKNOWN;
        this.architecture = architecture;
        this.lastSync = new Date(); // now
    }

}
