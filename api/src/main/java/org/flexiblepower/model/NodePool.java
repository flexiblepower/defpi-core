/**
 * File NodePool.java
 *
 * Copyright 2017 FAN
 */
package org.flexiblepower.model;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

/**
 * NodePool
 *
 * @version 0.1
 * @since 20 mrt. 2017
 */
@Entity
@Value
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class NodePool {

    @Id
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;

    private String name;

}
