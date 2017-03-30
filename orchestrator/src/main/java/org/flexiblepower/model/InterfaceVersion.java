/**
 * File InterfaceVersion.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.model;

import java.util.UUID;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

/**
 * InterfaceVersion
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 20 mrt. 2017
 */
@Getter
@Entity
public class InterfaceVersion {

    @Id
    private final ObjectId id;

    @JsonProperty("version_name")
    private final String versionName;

    @JsonProperty("accepts_hash")
    @Setter
    private String acceptsHash;

    @JsonProperty("sends_hash")
    @Setter
    private String sendsHash;

    public InterfaceVersion(final String name) {
        this.id = new ObjectId(UUID.randomUUID().toString());
        this.versionName = name;
    }

}
