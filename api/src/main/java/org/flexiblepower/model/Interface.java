/**
 * File Interface.java
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

import java.util.List;

import org.mongodb.morphia.annotations.Embedded;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

/**
 * Interface is the main class where is defined how the {@linkplain Service} objects may connect to one another. An
 * Interface may define multiple variants as a different {@linkplain InterfaceVersion}.
 *
 * @author coenvl
 * @version 0.1
 * @since Mar 20, 2017
 */
@Value
@Embedded
@AllArgsConstructor
@NoArgsConstructor(force = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Interface {

    private final String id;
    private final String name;
    private final String serviceId;

    @Embedded
    private final List<InterfaceVersion> interfaceVersions;

    private final boolean allowMultiple;
    private final boolean autoConnect;

    /**
     * Returns true if this interface is compatible with another. In other words, if any {@linkplain InterfaceVersion}
     * of this interface is the mirror of any {@linkplain InterfaceVersion} of the other interface in terms of the
     * send/receive hashes.
     * 
     * @param other The interface to compare with
     * @return true if for any interface version the send/receive hash pair matches any of the other interface's version
     *         receive/send hash pair
     */
    public boolean isCompatibleWith(final Interface other) {
        if (this.interfaceVersions != null) {
            for (final InterfaceVersion iv : this.interfaceVersions) {
                for (final InterfaceVersion oiv : other.getInterfaceVersions()) {
                    if (iv.isCompatibleWith(oiv)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * From this interface, find the {@linkplain InterfaceVersion} with the provided name
     * 
     * @param interfaceName The name of the required interface
     * @return The interface version with the provided name, or null if there is no such interface version
     */
    public InterfaceVersion getInterfaceVersionByName(final String interfaceName) {
        if (this.interfaceVersions != null) {
            for (final InterfaceVersion iv : this.interfaceVersions) {
                if (interfaceName.equals(iv.getVersionName())) {
                    return iv;
                }
            }
        }
        return null;
    }

}
