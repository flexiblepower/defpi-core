package org.flexiblepower.orchestrator;

import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Protos {

    final static Logger logger = LoggerFactory.getLogger(Containers.class);
    private final MongoDbConnector d;

    public Protos(final ObjectId user) {
        this.d = new MongoDbConnector();
        this.d.setApplicationUser(user);
    }

    public Protos() {
        this.d = new MongoDbConnector();
    }

    public List<Document> getProtos() {
        return this.d.getProtos();
    }

    public Document getProto(final String sha256) {
        return this.d.getProto(sha256);
    }

    public void insertProto(final String name, final String sha256, final String proto) {
        if (this.getProto(sha256) == null) {
            this.d.insertProto(name, sha256, proto);
        }
    }

    public void deleteProto(final String sha256) {
        this.d.deleteProto(sha256);
    }
}
