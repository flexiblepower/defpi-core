package org.flexiblepower.model;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity
public class Connection {

    @Embedded
    @Data
    public static class Endpoint {

        public Endpoint(final ObjectId processId, final String interfaceId) {
            this.processId = processId;
            this.interfaceId = interfaceId;
        }

        @JsonSerialize(using = ToStringSerializer.class)
        @JsonDeserialize(using = ObjectIdDeserializer.class)
        private final ObjectId processId;

        private final String interfaceId;

        /**
         * The version that is actually used. The value is set by the
         * ConnectionManager.
         */
        private String interfaceVersionName;

        /**
         * The port that the process will listen for incoming connections on.
         * The value is set by the ConnectionManager.
         */
        private int listenPort;

        @Override
        public String toString() {
            return String.format("Endpoint [processId=%s, listenPort=%s]", this.processId, this.listenPort);
        }

    }

    @Id
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    private ObjectId id = null;

    @Embedded
    private Endpoint endpoint1;

    @Embedded
    private Endpoint endpoint2;

    public Endpoint getOtherEndpoint(final Endpoint e) {
        if (e.equals(this.endpoint1)) {
            return this.endpoint2;
        } else if (e.equals(this.endpoint2)) {
            return this.endpoint1;
        } else {
            throw new IllegalArgumentException("The provided endpoint is not part of this connection");
        }
    }

    public Endpoint getEndpointForProcess(final ObjectId processId) {
        if (processId.equals(this.endpoint1.getProcessId())) {
            return this.endpoint1;
        } else if (processId.equals(this.endpoint2.getProcessId())) {
            return this.endpoint2;
        } else {
            throw new IllegalArgumentException("The provided processId is not part of this connection");
        }
    }

    @Override
    public String toString() {
        return String.format("Connection [id=%s, endpoint1=%s, endpoint2=%s]", this.id, this.endpoint1, this.endpoint2);
    }

}
