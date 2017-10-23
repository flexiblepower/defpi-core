package org.flexiblepower.connectors;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.types.ObjectId;
import org.flexiblepower.exceptions.InvalidObjectIdException;
import org.flexiblepower.model.Connection;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.UnidentifiedNode;
import org.flexiblepower.model.User;
import org.flexiblepower.orchestrator.pendingchange.PendingChange;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public final class MongoDbConnector {

    // private final static String host = "efpi-rd1.sensorlab.tno.nl";
    public final static String MONGO_HOST_KEY = "MONGO_HOST";
    public final static String MONGO_HOST_DFLT = "localhost";
    public final static String MONGO_PORT_KEY = "MONGO_PORT";
    public final static String MONGO_PORT_DFLT = "27017";
    private final static String MONGO_DATABASE_KEY = "MONGO_DATABASE";
    private final static String MONGO_DATABASE_DFLT = "def-pi";
    private static final long PENDING_CHANGE_TIMEOUT_MS = 5 * 60 * 1000;

    private static MongoDbConnector instance = null;

    private final MongoClient client;
    private final Datastore datastore;

    private String mongoDatabase;
    private String mongoHost;
    private String mongoPort;

    private MongoDbConnector() {
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

    public synchronized static MongoDbConnector getInstance() {
        if (MongoDbConnector.instance == null) {
            MongoDbConnector.instance = new MongoDbConnector();
        }
        return MongoDbConnector.instance;
    }

    public void close() {
        this.client.close();
    }

    public List<Process> listProcessesForUser(final User user) {
        final Query<Process> query = this.datastore.find(Process.class);
        query.criteria("userId").equal(user.getId());
        return query.asList();
    }

    /**
     * @return a list of all connections that are connected to the process with the provided id
     */
    public List<Connection> getConnectionsForProcess(final Process process) {
        final Query<Connection> q = this.datastore.find(Connection.class);
        q.or(q.criteria("endpoint1.processId").equal(process.getId()),
                q.criteria("endpoint2.processId").equal(process.getId()));
        return q.asList();
    }

    /**
     * Removes all connections that are connected to the process with the provided id from the database.
     *
     * @param processId
     */
    public void deleteConnectionsForProcess(final Process process) {
        final Query<Connection> q = this.datastore.find(Connection.class);
        q.or(q.criteria("container1").equal(process.getId()), q.criteria("container2").equal(process.getId()));
        this.datastore.delete(q);
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

    public <T> T get(final Class<T> type, final String id) throws InvalidObjectIdException {
        return this.datastore.get(type, MongoDbConnector.stringToObjectId(id));
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
        try {
            final ObjectMapper om = new ObjectMapper();
            return om.readValue(filters, new TypeReference<Map<String, Object>>() {
                // Map of String -> Object
            });
        } catch (final IOException e) {
            MongoDbConnector.log.error("Unable to parse filters");
            return Collections.emptyMap();
        }
    }

    /*
     * public static Map<String, Object> parseFilters(final String filters) {
     * final Gson gson = new GsonBuilder().create();
     *
     * @SuppressWarnings("unchecked")
     * final Map<String, Object> filter = gson.fromJson(filters, Map.class);
     * return filter;
     * }
     */

    /**
     * @param entity
     * @return the new objectId of the stored entity
     */
    public ObjectId save(final Object entity) {
        return (ObjectId) this.datastore.save(entity).getId();
    }

    public void delete(final Object entity) {
        this.datastore.delete(entity);
    }

    public void delete(final Class<?> type, final ObjectId id) {
        this.datastore.delete(type, id);
    }

    public void delete(final Class<?> type, final String id) throws InvalidObjectIdException {
        this.delete(type, MongoDbConnector.stringToObjectId(id));
    }

    /**
     * Private function that throws an exception if the string is not a valid ObjectId, and returns the corresponding
     * ObjectId otherwise.
     *
     * @param userId
     * @return
     * @throws InvalidObjectIdException
     */
    public static ObjectId stringToObjectId(final String id) throws InvalidObjectIdException {
        if (!ObjectId.isValid(id)) {
            throw new InvalidObjectIdException("The provided id is not a valid ObjectId");
        }
        return new ObjectId(id);
    }

    /**
     * This is essentially a "login" action, in which the user obtains from the database his user information.
     *
     * @param username
     * @param password
     * @return the user that is stored in the database that has the provided user name and password
     */
    public User getUser(final String username, final String password) {
        MongoDbConnector.log.trace("Searching for user with name {} and matching password", username);
        if ((username == null) || username.isEmpty() || (password == null) || password.isEmpty()) {
            return null;
        }

        final Query<User> query = this.datastore.find(User.class);
        query.and(query.criteria("username").equal(username),
                query.criteria("passwordHash").equal(User.computeUserPass(username, password)));
        return query.get();
    }

    public User getUserByUsername(final String username) {
        final Query<User> q = this.datastore.find(User.class);
        q.criteria("username").equal(username);
        return q.get();
    }

    public UnidentifiedNode getUnidentifiedNodeByDockerId(final String dockerId) {
        final Query<UnidentifiedNode> q = this.datastore.find(UnidentifiedNode.class);
        q.criteria("dockerId").equal(dockerId);
        return q.get();
    }

    /**
     * Retrieve the next PendingChange. It uses the findAndModify option to make sure that no tasks gets taken from the
     * queue twice.
     *
     * See http://www.programcreek.com/java-api-examples/index.php?api=com.google.code.morphia.query.UpdateOperations
     * for the polymorphism trick.
     *
     * @return The next unobtained PendingChange, null if there are no pendingChanges
     */
    public PendingChange getNextPendingChange() {
        final Query<PendingChange> query = this.datastore.createQuery(PendingChange.class)
                .field("obtainedAt")
                .equal(null) // Must be null
                .field("state")
                .notEqual(PendingChange.State.FAILED_PERMANENTLY) // Not failed
                .field("runAt")
                .lessThanOrEq(new Date()) // No future task
                .order("runAt")
                .disableValidation();
        final UpdateOperations<PendingChange> update = this.datastore.createUpdateOperations(PendingChange.class)
                .set("obtainedAt", new Date());
        return this.datastore.findAndModify(query, update);
    }

    public PendingChange getAbendonedPendingChanges() {
        return this.datastore.createQuery(PendingChange.class)
                .filter("obtainedAt <",
                        new Date(System.currentTimeMillis() - MongoDbConnector.PENDING_CHANGE_TIMEOUT_MS))
                .get();
    }
}
