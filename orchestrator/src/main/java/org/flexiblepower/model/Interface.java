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
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Interface
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 20 mrt. 2017
 */
@Getter
@Setter
@ToString
@Entity
public class Interface {

    @Id
    private final ObjectId id;

    @JsonProperty("name")
    private final String name;

    @Embedded
    @JsonProperty("interfaceVersions")
    private List<InterfaceVersion> interfaceVersions;

    @JsonProperty("cardinality")
    private int cardinality;

    @JsonProperty("autoConnect")
    private boolean autoConnect;

    @JsonProperty("subscribeHash")
    private String subscribeHash;

    @JsonProperty("publishHash")
    private String publishHash;

    public Interface(final String name) {
        this.id = new ObjectId(UUID.randomUUID().toString());
        this.name = name;
    }
}
