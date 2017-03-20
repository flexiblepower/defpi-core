package org.flexiblepower.orchestrator;

import java.io.Closeable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;

import org.bson.types.ObjectId;
import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.model.Connection;
import org.flexiblepower.model.Node;
import org.flexiblepower.model.User;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.query.Query;

import com.mongodb.MongoClient;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Maarten Kollenstart
 *         MongoDbConnector class that communicates with the MongoDB.
 *         The only method with a bit of logic is the linkAllowed method.
 */
@Slf4j
public class MongoDbConnector implements Closeable {

    // private final static String host = "efpi-rd1.sensorlab.tno.nl";
    private final static String MONGO_DB_HOST = "192.168.137.111";
    private final static String MONGO_DB_NAME = "def-pi";

    private final MongoClient client;
    private final Datastore datastore;

    // This is the user of the application, and accordingly, decides what functions are available
    private User appUser;

    public MongoDbConnector() {
        MongoDbConnector.log.info("Connecting to MongoDB on {}", MongoDbConnector.MONGO_DB_HOST);
        this.client = new MongoClient(MongoDbConnector.MONGO_DB_HOST);

        // Instantiate Morphia where to find your classes can be called multiple times with different packages or
        // classes
        final Morphia morphia = new Morphia();
        morphia.mapPackage("org.flexiblepower.model");

        // create the Datastore connecting to the default port on the local host
        this.datastore = morphia.createDatastore(this.client, MongoDbConnector.MONGO_DB_NAME);
        this.datastore.ensureIndexes();
    }

    @Override
    public void close() {
        this.client.close();
    }

    public void setApplicationUser(final User currentUser) {
        this.appUser = currentUser;
    }

    public void updateUser(final User user) throws AuthorizationException {
        if (this.appUser.isAdmin() || this.appUser.equals(user)) {
            this.datastore.save(user);
        } else {
            throw new AuthorizationException();
        }
    }

    public User getUser(final String username, final String password) {
        final Query<User> query = this.datastore.find(User.class);
        query.and(query.criteria("name").equal(username), query.criteria("password").equal(MongoDbConnector.sha256(password)));
        return query.get();
    }

    public void insertUser(final String username, final String password) throws AuthorizationException {
        if (this.appUser.isAdmin()) {
            this.datastore.save(new User(username, MongoDbConnector.sha256(password)));
        } else {
            throw new AuthorizationException();
        }
    }

    public void addUser(final User user) {
        this.datastore.save(user);
    }

    public void deleteUser(final String uid) {
        this.datastore.delete(User.class, new ObjectId(uid));
    }

    public List<Node> getNodes() {
        MongoDbConnector.log.info("Fetching containers from DB");
        final Query<Node> nodes = this.datastore.find(Node.class);

        return nodes.asList();
    }

    public Node getNode(final String uuid) {
        MongoDbConnector.log.info("Searching Node {} from DB", uuid);
        return this.datastore.get(Node.class, new ObjectId(uuid));
    }

    public Collection<Connection> getConnections() {
        MongoDbConnector.log.info("Fetching connections from DB");
        final Query<Connection> links = this.datastore.find(Connection.class);

        return links.asList();
    }

    public Connection getConnection(final String uuid) {
        MongoDbConnector.log.info("Searching Link {} from DB", uuid);
        return this.datastore.get(Connection.class, new ObjectId(uuid));
    }

    public List<Connection> getLinkForProces(final String uuid) {
        final Query<Connection> q = this.datastore.find(Connection.class);
        q.or(q.criteria("container2").equal(uuid), q.criteria("container2").equal(uuid));
        return q.asList();
    }

    public void insertLink(final Connection link) {
        this.datastore.save(link);
    }

    public void deleteLink(final String id) {
        this.datastore.delete(Connection.class, new ObjectId(id));
    }

    public void deleteLinksForProces(final String uuid) {
        final Query<Connection> q = this.datastore.find(Connection.class);
        q.or(q.criteria("container2").equal(uuid), q.criteria("container2").equal(uuid));
        this.datastore.delete(q);
    }

    // @SuppressWarnings("unchecked")
    // public boolean linkAllowed(final Link link) {
    // final MongoCollection<Document> c = this.db.getCollection("links");
    // final Document c1 = this.getProces(link.getContainer1());
    // final Document c2 = this.getProces(link.getContainer2());
    // if ((c1 == null) || (c2 == null)) {
    // return false;
    // }
    // final List<Document> container1Interfaces = (List<Document>) c1.get("interfaces");
    // final List<Document> container2Interfaces = (List<Document>) c2.get("interfaces");
    // for (final Document i : container1Interfaces) {
    // if (this.checkInterface(c, link, i, link.getContainer1())) {
    // return false;
    // }
    // }
    // for (final Document i : container2Interfaces) {
    // if (this.checkInterface(c, link, i, link.getContainer2())) {
    // return false;
    // }
    // }
    //
    // return true;
    // }
    //
    // private boolean checkInterface(final MongoCollection<Document> c,
    // final Link link,
    // final Document i,
    // final String container) {
    // final String subscribeHash = i.getString("subscribeHash");
    // final String publishHash = i.getString("publishHash");
    // if (i.getInteger("cardinality") == 1) {
    // if ((subscribeHash.equals(link.getInterface1()) && publishHash.equals(link.getInterface2()))
    // || (subscribeHash.equals(link.getInterface2()) && publishHash.equals(link.getInterface1()))) {
    //
    // final Document search = new Document("$and",
    // Arrays.asList(
    // new Document("$or",
    // Arrays.asList(new Document("container1", container),
    // new Document("container2", container))),
    // new Document("$or",
    // Arrays.asList(
    // new Document("interface1", link.getInterface1()).append("interface2",
    // link.getInterface2()),
    // new Document("interface1", link.getInterface2()).append("interface2",
    // link.getInterface1())))));
    // if (c.find(search).first() != null) {
    // MongoDbConnector.log.info(" interface '" + i.getString("name") + "' is already connected");
    // return true;
    // }
    // }
    // }
    // return false;
    // }

    static String sha256(final String input) {
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("SHA-256");
            final byte[] result = mDigest.digest(input.getBytes());
            final StringBuffer sb = new StringBuffer();

            for (final byte element : result) {
                sb.append(Integer.toString((element & 0xff) + 0x100, 16).substring(1));
            }

            return sb.toString();
        } catch (final NoSuchAlgorithmException e) {
            MongoDbConnector.log.error("No SHA256 algorithm");
        }
        return "";
    }
}
