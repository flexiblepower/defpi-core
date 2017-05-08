package org.flexiblepower.plugin.servicegen.model;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Getter;

/**
 * InterfaceDescription
 *
 * @author coenvl
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
    public String name;

    /**
     * (Required)
     */
    @JsonProperty("allowMultiple")
    public boolean allowMultiple;

    /**
     * (Required)
     */
    @JsonProperty("autoConnect")
    public boolean autoConnect;

    @JsonProperty("interfaceVersions")
    @JsonDeserialize(as = java.util.LinkedHashSet.class)
    public Set<InterfaceVersionDescription> interfaceVersions = null;

}