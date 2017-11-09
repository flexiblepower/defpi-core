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
