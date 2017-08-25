package org.flexiblepower.model;

import java.util.Date;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.Value;

@Value
public class PendingChangeDescription {

    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;

    private String type;

    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId userId;

    private Date created;

    private String description;

    private int count;

    private String state;
}