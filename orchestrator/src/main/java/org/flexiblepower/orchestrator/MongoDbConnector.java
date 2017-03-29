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
import org.flexiblepower.model.PrivateNode;
import org.flexiblepower.model.PublicNode;
import org.flexiblepower.model.User;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.query.Query;

import com.mongodb.MongoClient;

import lombok.extern.slf4j.Slf4j;

/**
 * MongoDbConnector
 *
 * The MongoDBConnector takes care of writing and reading objects from and to the mongo database.
 *
 * @author coenvl
 * @version 0.1
 * @since Mar 29, 2017
 */
@Slf4j
public final class MongoDbConnector implements Closeable {

    // private final static String host = "efpi-rd1.sensorlab.tno.nl";
    private final static String MONGO_DB_HOST = "localhost";
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

    /**
     * Private function that only throws an exception if the current logged in user is not an admin.
     *
     * @throws AuthorizationException
     */
    private void assertUserIsAdmin() throws AuthorizationException {
        if ((this.appUser == null) || !this.appUser.isAdmin()) {
            throw new AuthorizationException();
        }
    }

    /**
     * Sets the provided user as the current "logged in" application user. This means that all subsequent calls will be
     * executed as the provided user.
     *
     * @param currentUser
     * @see {@link #getUser(String, String)}
     */
    public void setApplicationUser(final User currentUser) {
        this.appUser = currentUser;
    }

    /**
     * Updates the user information in the database. This function can only be used by that user, or by users with
     * administrator rights.
     *
     * @param user
     * @return the (new) userId of the updated user
     * @throws AuthorizationException
     */
    public String updateUser(final User user) throws AuthorizationException {
        MongoDbConnector.log.debug("Updating user: {}", user);
        if (this.appUser.isAdmin() || this.appUser.equals(user)) {
            return this.datastore.save(user).getId().toString();
        } else {
            throw new AuthorizationException();
        }
    }

    /**
     * @return a list of all users currently stored in the mongo db.
     * @throws AuthorizationException
     */
    public List<User> getUsers() throws AuthorizationException {
        MongoDbConnector.log.debug("Listing all users {}");
        this.assertUserIsAdmin();
        return this.datastore.find(User.class).asList();
    }

    /**
     * Get a user object from the database that has the provided userId, or null if no such user exists. This function
     * can only be used by a users with administrator rights.
     *
     * @param userId
     * @return the user stored with the provided Id, or null
     * @throws AuthorizationException
     */
    public User getUser(final String userId) throws AuthorizationException {
        MongoDbConnector.log.debug("Searching user with id {}", userId);
        this.assertUserIsAdmin();
        return this.datastore.get(User.class, new ObjectId(userId));
    }

    /**
     * This is essentially a "login" action, in which the user obtains from the database his user information. However,
     * note that it does not automatically sets this user as the current application user; this is only achieved using
     * the {@linkplain #setApplicationUser(User)} function.
     *
     * @see {@link #setApplicationUser(User)}
     * @param username
     * @param password
     * @return the user that is stored in the database that has the provided user name and password
     */
    public User getUser(final String username, final String password) {
        MongoDbConnector.log.debug("Searching for user with name {} and matching password", username);
        if ((username == null) || username.isEmpty() || (password == null) || password.isEmpty()) {
            return null;
        }

        final Query<User> query = this.datastore.find(User.class);
        query.and(query.criteria("name").equal(username),
                query.criteria("password").equal(MongoDbConnector.sha256(password)));
        return query.get();
    }

    /**
     * This function creates a new user with the provided name and password. This is essentially a "registration"
     * function. Note that it only inserts the new user in the database, and does not load this user as the new
     * application user.
     *
     * @param username
     * @param password
     * @return the userId of the newly created user
     * @throws AuthorizationException
     */
    public String createNewUser(final String username, final String password) throws AuthorizationException {
        MongoDbConnector.log.info("Registering new user with name {} and password", username);
        return this.datastore.save(new User(username, MongoDbConnector.sha256(password))).getId().toString();
    }

    /**
     * Inserts a user object in the database. This function can only be used by a users with administrator rights.
     *
     * @param user
     * @return the userId of the inserted user
     * @throws AuthorizationException if the current logged in user does not have admin rights
     */
    public String insertUser(final User user) throws AuthorizationException {
        MongoDbConnector.log.debug("Adding new user: {}", user);
        this.assertUserIsAdmin();
        return this.datastore.save(user).getId().toString();
    }

    /**
     * Deletes a user object from the database that has the provided userId. This function can only be used by a users
     * with administrator rights.
     *
     * @param userId
     * @throws AuthorizationException
     */
    public void deleteUser(final String userId) throws AuthorizationException {
        MongoDbConnector.log.debug("Removing user with id {}", userId);
        this.assertUserIsAdmin();
        this.datastore.delete(User.class, new ObjectId(userId));
    }

    /**
     * @return a list of all nodes that are stored in the database.
     */
    public List<Node> getNodes() {
        MongoDbConnector.log.debug("Listing all nodes");
        return this.datastore.find(Node.class).asList();
    }

    /**
     * @return a list of all public nodes that are stored in the database
     */
    public List<PublicNode> getPublicNodes() {
        MongoDbConnector.log.debug("Listing all public nodes");
        return this.datastore.find(PublicNode.class).asList();
    }

    /**
     * @return a list of all private nodes that are stored in the database.
     */
    public List<PrivateNode> getPrivateNodes() {
        MongoDbConnector.log.debug("Listing all private nodes");
        // TODO: List only the private nodes from the current user OR all private nodes if the user is admin
        return this.datastore.find(PrivateNode.class).asList();
    }

    /**
     * Returns the public node that is stored in the database with the provided id, or null if no such node exists.
     *
     * @param nodeId
     * @return the public node that has the provided id, or null
     */
    public PublicNode getPublicNode(final String nodeId) {
        MongoDbConnector.log.debug("Searching PublicNode with id {}", nodeId);
        return this.datastore.get(PublicNode.class, new ObjectId(nodeId));
    }

    /**
     * Returns the private node that is stored in the database with the provided id, or null if no such node exists.
     *
     * @param nodeId
     * @return the private node that has the provided id, or null
     */
    public PrivateNode getPrivateNode(final String nodeId) {
        MongoDbConnector.log.debug("Searching PrivateNode with id {}", nodeId);
        return this.datastore.get(PrivateNode.class, new ObjectId(nodeId));
    }

    /**
     * Insert the provided node (public or private) in the database.
     *
     * @param node
     * @return the id of the newly inserted node
     */
    public String insertNode(final Node node) {
        MongoDbConnector.log.debug("Adding new node: {}", node);
        return this.datastore.save(node).getId().toString();
    }

    /**
     * Removes the node (either public or private) that has the provided id from the database.
     *
     * @param nodeId
     */
    public void deleteNode(final String nodeId) {
        MongoDbConnector.log.debug("Deleting node with id {}", nodeId);
        this.datastore.delete(Node.class, new ObjectId(nodeId));
    }

    /**
     * @return a list of all connections that are stored in the database
     */
    public Collection<Connection> getConnections() {
        MongoDbConnector.log.debug("Listing all connections");
        return this.datastore.find(Connection.class).asList();
    }

    /**
     * Returns the connection that is stored in the database with the provided id, or null if no such connection exists.
     *
     * @param connectionId
     * @return the connection that has the provided id, or null
     */
    public Connection getConnection(final String connectionId) {
        MongoDbConnector.log.debug("Searching connection with id {} ", connectionId);
        return this.datastore.get(Connection.class, new ObjectId(connectionId));
    }

    /**
     * @return a list of all connections that are connected to the process with the provided id
     */
    public List<Connection> getConnectionsForProcess(final String processId) {
        MongoDbConnector.log.debug("Listing all connections for process with id {}", processId);
        final Query<Connection> q = this.datastore.find(Connection.class);
        q.or(q.criteria("container2").equal(processId), q.criteria("container2").equal(processId));
        return q.asList();
    }

    /**
     * Insert the provided connection in the database.
     *
     * @param connection
     * @return the id of the newly inserted connection
     */
    public String insertConnection(final Connection connection) {
        MongoDbConnector.log.debug("Adding new connection: {}", connection);
        return this.datastore.save(connection).getId().toString();
    }

    /**
     * Removes the connection that has the provided id from the database.
     *
     * @param connectionId
     */
    public void deleteConnection(final String connectionId) {
        MongoDbConnector.log.debug("Deleting connection with id {}", connectionId);
        this.datastore.delete(Connection.class, new ObjectId(connectionId));
    }

    /**
     * Removes all connections that are connected to the process with the provided id from the database.
     *
     * @param processId
     */
    public void deleteConnectionsForProcess(final String processId) {
        MongoDbConnector.log.debug("Deleting all connections for process with id {}", processId);
        final Query<Connection> q = this.datastore.find(Connection.class);
        q.or(q.criteria("container2").equal(processId), q.criteria("container2").equal(processId));
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

    /**
     * Generate the sha256 hash for a given string
     *
     * @param input
     * @return the hash of the input string
     */
    private static String sha256(final String input) {
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
