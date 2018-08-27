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
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Builder;
import lombok.Value;

/**
 * The service is the type of a {@link Process}, and defines what {@link Interface} the process may have. A service is
 * defined in a docker image, and correspondingly, a Process is a container that uses that image.
 * <p>
 * Comparing dEF-Pi to Java, the Service is a class, and the Process is the object.
 *
 * @version 0.1
 * @since Mar 30, 2017
 */
@Value
@Builder
public class Service {

    private final String name;

    /**
     * dEF-Pi interfaces
     */
    private final Set<Interface> interfaces;

    private final String registry;

    private final String repository;

    private final String id;

    private final String version;

    private final Date created;

    private final Map<Architecture, String> tags;

    /**
     * Get the full docker image name of this service including the repository and the tag
     * 
     * @param architecture The architecture of the system that we want the image name for
     * @return A string that identifies the docker image that contains this service.
     */
    @JsonIgnore
    public String getFullImageName(final Architecture architecture) {
        return this.registry + "/" + this.repository + "/" + this.getImageName() + ":"
                + (this.tags == null ? this.version : this.tags.get(architecture));
    }

    /**
     * @return The docker image name without the repository or tag
     */
    @JsonIgnore
    public String getImageName() {
        return this.id.split(":")[0];
    }

    /**
     * Based on the tag of an image name, deduce the architecture that is used in an image
     * 
     * @param tag Only the tag of the image
     * @return The architecture that was used in the image, or UNKNOWN if we cannot deduce it.
     */
    public static Architecture getArchitectureFromTag(final String tag) {
        if (tag.endsWith("-arm")) {
            return Architecture.ARM;
        } else if (!tag.contains("-")) {
            // 64bit is default
            return Architecture.X86_64;
        } else {
            // There is an architecture defined, but we don't know it
            return Architecture.UNKNOWN;
        }
    }

    /**
     * Get an interface from this service, based on the interfaceID.
     * 
     * @param interfaceId The ID of the interface to look for
     * @return The Interface with the provided ID, or null if the service does not provide it.
     */
    public final Interface getInterface(final String interfaceId) {
        for (final Interface itf : this.interfaces) {
            if (itf.getId().equals(interfaceId)) {
                return itf;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "Service [" + this.getFullImageName(Architecture.X86_64) + ", version=" + this.version + "]";
    }

}
