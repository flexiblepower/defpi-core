package org.flexiblepower.model;

import java.util.Date;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
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