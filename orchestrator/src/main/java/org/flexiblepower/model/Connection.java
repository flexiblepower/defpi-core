package org.flexiblepower.model;

import java.util.UUID;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Connection {

    @Id
    private ObjectId id;

    private String container1;
    private String container2;
    private String interface1;
    private String interface2;

    public Connection(final String container1,
            final String container2,
            final String interface1,
            final String interface2) {
        this.id = new ObjectId(UUID.randomUUID().toString());
        this.container1 = container1;
        this.container2 = container2;
        this.interface1 = interface1;
        this.interface2 = interface2;
    }

}
