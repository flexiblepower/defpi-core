package org.flexiblepower.orchestrator;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.types.ObjectId;
import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.InvalidObjectIdException;
import org.flexiblepower.model.Connection;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.UnidentifiedNode;
import org.flexiblepower.model.User;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.query.Query;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
    private final static String MONGO_HOST_KEY = "MONGO_HOST";
    private final static String MONGO_HOST_DFLT = "172.17.0.3";
    private final static String MONGO_PORT_KEY = "MONGO_PORT";
    private final static String MONGO_PORT_DFLT = "27017";
    private final static String MONGO_DATABASE_KEY = "MONGO_DATABASE";
    private final static String MONGO_DATABASE_DFLT = "def-pi";

    private final MongoClient client;
    private final Datastore datastore;

    // This is the user of the application, and accordingly, decides what functions are available
    private User appUser;
    private String mongoDatabase;
    private String mongoHost;
    private String mongoPort;

    public MongoDbConnector() {
        this.mongoHost = System.getenv(MongoDbConnector.MONGO_HOST_KEY);
        if (this.mongoHost == null) {
            this.mongoHost = MongoDbConnector.MONGO_HOST_DFLT;
        }
        this.mongoDatabase = System.getenv(MongoDbConnector.MONGO_DATABASE_KEY);
        if (this.mongoDatabase == null) {
            this.mongoDatabase = MongoDbConnector.MONGO_DATABASE_DFLT;
        }
        this.mongoPort = System.getenv(MongoDbConnector.MONGO_PORT_KEY);
        if (this.mongoPort == null) {
            this.mongoPort = MongoDbConnector.MONGO_PORT_DFLT;
        }

        MongoDbConnector.log.info("Connecting to MongoDB on {}:{}", this.mongoHost, this.mongoPort);
        this.client = new MongoClient(this.mongoHost, Integer.parseInt(this.mongoPort));

        // Instantiate Morphia where to find your classes can be called multiple times with different packages or
        // classes
        final Morphia morphia = new Morphia();
        morphia.mapPackage("org.flexiblepower.model");

        // create the Datastore connecting to the default port on the local host
        this.datastore = morphia.createDatastore(this.client, this.mongoDatabase);
        this.datastore.ensureIndexes();
    }

    @Override
    public void close() {
        this.client.close();
    }

    public <T> List<T> list(final Class<T> type,
            final int page,
            final int perPage,
            final String sortDir,
            final String sortField,
            final Map<String, Object> filter) {
        final String sortSign = ("DESC".equals(sortDir)) ? "-" : "";
        Query<T> query = this.datastore.createQuery(type);
        query.disableValidation();
        for (final Entry<String, Object> e : filter.entrySet()) {
            query.filter(e.getKey(), e.getValue());
        }
        query = query.order(sortSign + sortField).offset((page - 1) * perPage).limit(perPage);
        return query.asList();
    }

    public <T> List<T> list(final Class<T> type) {
        return this.datastore.find(type).asList();
    }

    public <T> T get(final Class<T> type, final ObjectId id) {
        return this.datastore.get(type, id);
    }

    public <T> int totalCount(final Class<T> type, final Map<String, Object> filter) {
        final Query<T> query = this.datastore.createQuery(type);
        query.disableValidation();
        for (final Entry<String, Object> e : filter.entrySet()) {
            query.filter(e.getKey(), e.getValue());
        }
        return (int) query.countAll();
    }

    public static Map<String, Object> parseFilters(final String filters) {
        final Gson gson = new GsonBuilder().create();
        @SuppressWarnings("unchecked")
        final Map<String, Object> filter = gson.fromJson(filters, Map.class);
        return filter;
    }

    public void save(final Object entity) {
        this.datastore.save(entity);
    }

    public void delete(final Object entity) {
        this.datastore.delete(entity);
    }

    /**
     * Private function that throws an exception if the string is not a valid ObjectId, and returns the corresponding
     * ObjectId otherwise.
     *
     * @param userId
     * @return
     * @throws InvalidObjectIdException
     */
    private static ObjectId stringToObjectId(final String id) throws InvalidObjectIdException {
        if (!ObjectId.isValid(id)) {
            throw new InvalidObjectIdException("The provided id is not a valid ObjectId");
        }
        return new ObjectId(id);
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
        return this.datastore.find(User.class).asList();
    }

    /**
     * Get a user object from the database that has the provided userId, or null if no such user exists. This function
     * can only be used by a users with administrator rights.
     *
     * @param userId
     * @return the user stored with the provided Id, or null
     * @throws AuthorizationException
     * @throws InvalidObjectIdException
     */
    public User getUser(final String userId) throws AuthorizationException, InvalidObjectIdException {
        final ObjectId id = MongoDbConnector.stringToObjectId(userId);
        return this.getUser(id);
    }

    /**
     * Get a user object from the database that has the provided userId, or null if no such user exists. This function
     * can only be used by a users with administrator rights.
     *
     * @param userId
     * @return the user stored with the provided Id, or null
     * @throws AuthorizationException
     * @throws InvalidObjectIdException
     */
    public User getUser(final ObjectId userId) {
        MongoDbConnector.log.debug("Searching user with id {}", userId.toString());
        return this.datastore.get(User.class, userId);
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
        query.and(query.criteria("username").equal(username),
                query.criteria("passwordHash").equal(User.computeUserPass(username, password)));
        return query.get();
    }

    public List<Process> listProcessesForUser(final User user) {
        final Query<Process> query = this.datastore.find(Process.class);
        query.criteria("userId").equal(user.getId());
        return query.asList();
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
        return this.datastore.save(new User(username, password)).getId().toString();
    }

    /**
     * Inserts a user object in the database. This function can only be used by a users with administrator rights.
     *
     * @param user
     * @return the userId of the inserted user
     * @throws AuthorizationException if the current logged in user does not have admin rights
     */
    public String saveUser(final User user) throws AuthorizationException {
        MongoDbConnector.log.debug("Saving user: {}", user);
        return this.datastore.save(user).getId().toString();
    }

    /**
     * Deletes a user object from the database that has the provided userId. This function can only be used by a users
     * with administrator rights.
     *
     * @param userId
     * @throws AuthorizationException
     * @throws InvalidObjectIdException
     */
    public void deleteUser(final String userId) throws AuthorizationException, InvalidObjectIdException {
        MongoDbConnector.log.debug("Removing user with id {}", userId);
        final ObjectId id = MongoDbConnector.stringToObjectId(userId);
        this.datastore.delete(User.class, id);
    }

    /**
     * @return a list of all connections that are stored in the database
     */
    public List<Connection> getConnections() {
        MongoDbConnector.log.debug("Listing all connections");
        return this.datastore.find(Connection.class).asList();
    }

    public UnidentifiedNode getUnidentifiedNodeByDockerId(final String dockerId) {
        final Query<UnidentifiedNode> q = this.datastore.find(UnidentifiedNode.class);
        q.criteria("dockerId").equal(dockerId);
        return q.get();
    }

    /**
     * Returns the connection that is stored in the database with the provided id, or null if no such connection exists.
     *
     * @param connectionId
     * @return the connection that has the provided id, or null
     * @throws InvalidObjectIdException
     */
    public Connection getConnection(final String connectionId) throws InvalidObjectIdException {
        MongoDbConnector.log.debug("Searching connection with id {} ", connectionId);
        final ObjectId id = MongoDbConnector.stringToObjectId(connectionId);
        return this.datastore.get(Connection.class, id);
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
     * @throws InvalidObjectIdException
     */
    public void deleteConnection(final String connectionId) throws InvalidObjectIdException {
        MongoDbConnector.log.debug("Deleting connection with id {}", connectionId);
        final ObjectId id = MongoDbConnector.stringToObjectId(connectionId);
        this.datastore.delete(Connection.class, id);
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

}
