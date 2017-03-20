/**
 * File Interface.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.model;

import java.util.List;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Id;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Interface
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 20 mrt. 2017
 */
public class Interface {

    @Id
    private final ObjectId id;

    @JsonProperty("name")
    private final String name;

    @Embedded
    @JsonProperty("interface_versions")
    private List<InterfaceVersion> interfaceVersions;

    public Interface(final String name) {
        this.id = new ObjectId(UUID.randomUUID().toString());
        this.name = name;
    }
}
