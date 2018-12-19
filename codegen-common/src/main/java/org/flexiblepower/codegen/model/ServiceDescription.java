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

import org.flexiblepower.model.Parameter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Getter;
import lombok.Setter;

/**
 * ServiceDescription
 *
 * @version 0.1
 * @since Apr 13, 2017
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceDescription {

    /**
     * (Required)
     */
    @JsonProperty("name")
    private String name;

    @Setter
    @JsonProperty("id")
    private String id;

    /**
     * (Required)
     */
    @JsonProperty("version")
    private String version;

    @JsonProperty("description")
    private String description;

    @JsonProperty("iconURL")
    private String iconURL;

    @JsonProperty("interfaces")
    @JsonDeserialize(as = java.util.LinkedHashSet.class)
    private final Set<InterfaceDescription> interfaces = null;

    @JsonProperty("parameters")
    @JsonDeserialize(as = java.util.LinkedHashSet.class)
    private final Set<Parameter> parameters = null;
}
