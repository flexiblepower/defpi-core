/*-
 * #%L
 * dEF-Pi service codegen-common
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
package org.flexiblepower.codegen.model;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Getter;

/**
 * InterfaceDescription
 *
 * @version 0.1
 * @since Apr 13, 2017
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"name", "allowMultiple", "autoConnect", "interfaceVersions"})
public class InterfaceDescription {

    /**
     * (Required)
     */
    @JsonProperty("name")
    private String name;

    /**
     * (Required)
     */
    @JsonProperty("allowMultiple")
    private boolean allowMultiple;

    /**
     * (Required)
     */
    @JsonProperty("autoConnect")
    private boolean autoConnect;

    @JsonProperty("interfaceVersions")
    @JsonDeserialize(as = java.util.LinkedHashSet.class)
    private final Set<InterfaceVersionDescription> interfaceVersions = null;

    /**
     * Returns the dEF-Pi identifier of the provided interface, which equals to the name, but with spaces replaces by
     * dashes.
     *
     * @return A indentifier that is, for this service, unique for the interface.
     */
    @JsonIgnore
    public String getId() {
        return this.name.toLowerCase().replace(" ", "-");
    }

}
