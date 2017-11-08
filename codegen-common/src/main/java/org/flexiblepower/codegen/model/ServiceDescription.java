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

    @JsonProperty("interfaces")
    @JsonDeserialize(as = java.util.LinkedHashSet.class)
    private final Set<InterfaceDescription> interfaces = null;

    @JsonProperty("parameters")
    @JsonDeserialize(as = java.util.LinkedHashSet.class)
    private final Set<Parameter> parameters = null;
}
