/**
 * File Service.java
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
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Builder;
import lombok.Value;

/**
 * Service
 *
 * @version 0.1
 * @since Mar 30, 2017
 */
@Value
@Builder
public class Service {

    private final String name;

    /**
     * Def-pi interfaces
     */
    private final Set<Interface> interfaces;

    // /**
    // * Volume mappings
    // */
    // private Set<String> mappings;
    //
    // /**
    // * Physical ports to open
    // */
    // private Set<String> ports;

    private final String registry;

    private final String repository;

    private final String id;

    // private String tag;

    private final String version;

    private final Date created;

    private final Map<Architecture, String> tags;

    // public Service(final String name,
    // final Set<Interface> interfaces,
    // final String fullname,
    // final String version,
    // final Date created) {
    // this.name = name;
    // this.interfaces = interfaces;
    //
    // final int pReg = fullname.indexOf('/');
    // final int pTag = fullname.indexOf(':', pReg);
    // final int pHash = fullname.indexOf('@', pTag);
    //
    // this.registry = fullname.substring(0, pReg);
    // this.id = fullname.substring(pReg + 1, pTag);
    // final String tag = fullname.substring(pTag + 1, pHash);
    // this.tags.put(Service.getArchitectureFromTag(tag), tag);
    //
    // this.version = version;
    //
    // this.created = created;
    // }

    @JsonIgnore
    public String getFullImageName(final Architecture architecture) {
        return this.registry + "/" + this.repository + "/" + this.getImageName() + ":"
                + (this.tags == null ? this.version : this.tags.get(architecture));
    }

    @JsonIgnore
    public String getImageName() {
        return this.id.split(":")[0];
    }

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
     * @param interfaceId
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
