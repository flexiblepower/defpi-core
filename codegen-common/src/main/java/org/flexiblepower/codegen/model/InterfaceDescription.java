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

    @JsonIgnore
    public String getId() {
        return this.name.toLowerCase().replace(" ", "-");
    }

}