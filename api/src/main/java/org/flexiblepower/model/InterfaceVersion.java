/**
 * File InterfaceVersion.java
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

import org.mongodb.morphia.annotations.Embedded;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

/**
 * InterfaceVersion
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

    public boolean isCompatibleWith(final InterfaceVersion other) {
        return this.receivesHash.equals(other.getSendsHash()) && this.sendsHash.equals(other.getReceivesHash());
    }

    @Override
    public int compareTo(final InterfaceVersion o) {
        return this.versionName.compareTo(o.getVersionName());
    }

}
