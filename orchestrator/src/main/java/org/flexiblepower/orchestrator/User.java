package org.flexiblepower.orchestrator;

import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.glassfish.jersey.internal.util.Base64;

public class User {

    private final Database db = new Database();
    private Document user = null;

    public Document getUser(final HttpHeaders httpHeaders) {
        final MultivaluedMap<String, String> headers = httpHeaders.getRequestHeaders();
        if (headers.containsKey("authorization")) {
            final List<String> auths = headers.get("authorization");
            final byte[] auth = auths.get(0).substring(6).getBytes();
            final String[] credentials = new String(Base64.decode(auth)).split(":");
            this.user = this.db.getUser(credentials[0], credentials[1]);
            return this.user;
        }
        return null;
    }

    public ObjectId getUserId(final HttpHeaders httpHeaders) {
        if (this.getUser(httpHeaders) == null) {
            return null;
        }

        return this.user.getObjectId("_id");
    }

    public void addUser(final String username, final String password) {
        this.db.insertUser(username, password);
    }

}
