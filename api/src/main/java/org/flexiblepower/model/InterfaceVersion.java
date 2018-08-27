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

import org.mongodb.morphia.annotations.Embedded;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

/**
 * The InterfaceVersion represents a specific version of the {@link Interface}, with a particular send/receive hashpair.
 * This uniquely identifies what other InterfaceVersions are compatible with it. The InterfaceVersion is the specific
 * endpoint for a connection that allows a process to communicate with other processes.
 *
 * @version 0.1
 * @since 20 mrt. 2017
 * @see Interface
 */
@Value
@Embedded
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class InterfaceVersion implements Comparable<InterfaceVersion> {

    private String versionName;
    private String receivesHash;
    private String sendsHash;

    /**
     * Returns whether this interface version is compatible with another. This is true if and only if this version's send
     * hash equals the other version's receive hash and vice versa.
     * 
     * @param other The other interface version to check compatibility with
     * @return true if the other interface version is compatibly.
     */
    public boolean isCompatibleWith(final InterfaceVersion other) {
        return other != null && other.getSendsHash().equals(this.receivesHash) && other.getReceivesHash().equals(this.sendsHash);
    }

    @Override
    public int compareTo(final InterfaceVersion o) {
        return this.versionName == null ? -1 : this.versionName.compareTo(o.getVersionName());
    }

}
