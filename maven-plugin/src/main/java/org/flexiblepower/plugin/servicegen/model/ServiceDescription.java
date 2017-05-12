package org.flexiblepower.plugin.servicegen.model;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Getter;

/**
 * ServiceDescription
 *
 * @author coenvl
 * @version 0.1
 * @since Apr 13, 2017
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"name", "version", "interfaces"})
public class ServiceDescription {

    /**
     * (Required)
     */
    @JsonProperty("name")
    private String name;

    /**
     * (Required)
     */
    @JsonProperty("version")
    private String version;

    @JsonProperty("interfaces")
    @JsonDeserialize(as = java.util.LinkedHashSet.class)
    private final Set<InterfaceDescription> interfaces = null;
}
