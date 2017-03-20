package org.flexiblepower.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Host
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 20 mrt. 2017
 */
@Getter
@Setter
@ToString
public class Host {

    @Id
    private ObjectId id;

    private String state;
    private String hostname;
    private String ip;
    private String user;
    private Map<String, String> labels;

    public Host(final String id, final String hostname, final String ip) {
        this.id = new ObjectId(UUID.randomUUID().toString());
        this.hostname = hostname;
        this.ip = ip;
        this.labels = new HashMap<>();
    }
}
