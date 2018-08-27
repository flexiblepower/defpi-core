/*-
 * #%L
 * dEF-Pi REST Orchestrator
 * %%
 * Copyright (C) 2017 - 2018 Flexible Power Alliance Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.flexiblepower.connectors;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.bson.types.ObjectId;
import org.flexiblepower.exceptions.InvalidObjectIdException;
import org.flexiblepower.model.Connection;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.UnidentifiedNode;
import org.flexiblepower.model.User;
import org.flexiblepower.orchestrator.pendingchange.PendingChange;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;

import lombok.extern.slf4j.Slf4j;

/**
 * MongoDbConnector5
 *
 * The MongoDBConnector takes care of writing and reading objects from and to the mongo database.
 *
 * @version 0.1
 * @since Mar 29, 2017
 */
@Slf4j
public final class MongoDbConnector {

    /**
     * The key, or system variable name, which holds the hostname where the mongo db is available from the orchestrator
     * point of view
     */
    public final static String MONGO_HOST_KEY = "MONGO_HOST";
    private final static String MONGO_HOST_DFLT = "localhost";

    /**
     * The key, or system variable name, which holds the port where the mongo db runs as available from the orchestrator
     * point of view
     */
    public final static String MONGO_PORT_KEY = "MONGO_PORT";
    private final static String MONGO_PORT_DFLT = "27017";

    /**
     * The key, or system variable name, which holds the name of the database to use in the mongo db.
     */
    public final static String MONGO_DATABASE_KEY = "MONGO_DATABASE";
    private final static String MONGO_DATABASE_DFLT = "def-pi";

    private static final long PENDING_CHANGE_TIMEOUT_MS = Duration.ofMinutes(5).toMillis();

    private static MongoDbConnector instance = null;

    private final Datastore datastore;

    private MongoDbConnector() {
        String mongoHost = System.getenv(MongoDbConnector.MONGO_HOST_KEY);
        if (mongoHost == null) {
            mongoHost = MongoDbConnector.MONGO_HOST_DFLT;
        }
        String mongoDatabase = System.getenv(MongoDbConnector.MONGO_DATABASE_KEY);
        if (mongoDatabase == null) {
            mongoDatabase = MongoDbConnector.MONGO_DATABASE_DFLT;
        }
        String mongoPort = System.getenv(MongoDbConnector.MONGO_PORT_KEY);
        if (mongoPort == null) {
            mongoPort = MongoDbConnector.MONGO_PORT_DFLT;
        }

        MongoDbConnector.log.info("Connecting to MongoDB on {}:{}", mongoHost, mongoPort);
        @SuppressWarnings("resource")
        final MongoClient client = new MongoClient(mongoHost, Integer.parseInt(mongoPort));

        // Instantiate Morphia where to find your classes can be called multiple times with different packages or
        // classes
        final Morphia morphia = new Morphia();
        morphia.mapPackage("org.flexiblepower.model");

        // create the Datastore connecting to the default port on the local host
        this.datastore = morphia.createDatastore(client, mongoDatabase);
        this.datastore.ensureIndexes();
    }

    /**
     * @return The singleton instance of the MongoDbConnector
     */
    public static MongoDbConnector getInstance() {
        if (MongoDbConnector.instance == null) {
            MongoDbConnector.instance = new MongoDbConnector();
        }
        return MongoDbConnector.instance;
    }

    /**
     * @param user The user who is the owner of the list of processes
     * @return List of processes of a specific user
     */
    public List<Process> listProcessesForUser(final User user) {
        final Query<Process> query = this.datastore.find(Process.class);
        query.criteria("userId").equal(user.getId());
        return query.asList();
    }

    /**
     * @param process the process the list of connections are connected to
     * @return a list of all connections that are connected to the process with the provided id
     */
    public List<Connection> getConnectionsForProcess(final Process process) {
        final Query<Connection> q = this.datastore.find(Connection.class);
        q.or(q.criteria("endpoint1.processId").equal(process.getId()),
                q.criteria("endpoint2.processId").equal(process.getId()));
        return q.asList();
    }

    /**
     * List object; It is possible to paginate, sort and filter all objects depending on the provided arguments.
     *
     * @param type The type of object to retrieve
     * @param page The page to view
     * @param perPage The amount of objects to view per page, and thus the maximum amount of objects returned
     * @param sortDir The direction to sort
     * @param sortField The field to sort on
     * @param filter A key/value map of filters
     * @return A list all objects that match the filters, or a paginated subset thereof
     */
    public <T> List<T> list(final Class<T> type,
            final int page,
            final int perPage,
            final String sortDir,
            final String sortField,
            final Map<String, Object> filter) {
        final Query<T> query = this.datastore.createQuery(type);
        query.disableValidation();

        // Filter
        for (final Entry<String, Object> entry : filter.entrySet()) {
            final String pattern = entry.getValue().toString();
            if ((pattern.charAt(0) == '/') && (pattern.charAt(pattern.length() - 1) == '/')) {
                query.filter(entry.getKey(), Pattern.compile(pattern.substring(1, pattern.length() - 1)));
            } else {
                query.filter(entry.getKey(), entry.getValue());
            }
        }

        // Sort
        if ((sortField != null) && !sortField.isEmpty()) {
            final String sortSign = ("DESC".equals(sortDir)) ? "-" : "";
            query.order(sortSign + sortField);
        }

        // Paginate
        final FindOptions opts = new FindOptions().skip((page - 1) * perPage).limit(perPage);
        return query.asList(opts);
    }

    /**
     * List object of a specific type
     *
     * @param type The type of object to retrieve
     * @return A list of all objects in the mongo db of the specified type
     */
    public <T> List<T> list(final Class<T> type) {
        return this.datastore.find(type).asList();
    }

    /**
     * Get object with a specific object id
     *
     * @param type The type of object to retrieve
     * @param id The ObjectId to search for
     * @return A list of all objects in the mongo db of the specified type
     */
    public <T> T get(final Class<T> type, final ObjectId id) {
        return this.datastore.get(type, id);
    }

    /**
     * Get a list of objects with thea specific object ids
     *
     * @param type The type of object to retrieve
     * @param ids The ObjectIds to search for
     * @return A list of the specified objects in the mongo db
     */
    public <T> List<T> get(final Class<T> type, final List<ObjectId> ids) {
        return this.datastore.get(type, ids).asList();
    }

    /**
     * Count all objects of a particular type currently stored in the database; possibly count the subset that match a
     * set of criteria
     *
     * @param type The type of object to count
     * @param filter A filter to count a specific filtered subset of objects, may be empty
     * @return The number of objects that match the filter
     */
    public <T> int totalCount(final Class<T> type, final Map<String, Object> filter) {
        final Query<T> query = this.datastore.createQuery(type);
        query.disableValidation();
        for (final Entry<String, Object> e : filter.entrySet()) {
            query.filter(e.getKey(), e.getValue());
        }
        return (int) query.count(); // All();
    }

    /**
     * Create the Map of key/value pairs that will act as filters on any
     * {@link #list(Class, int, int, String, String, Map)} or {@link #totalCount(Class, Map)} function. By using the map
     * representation it is easier to add filters in an earlier stage.
     *
     * @param filters The JSON representation of a key/value map
     * @return The {@link Map} with the given key/value pairs
     */
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

    /**
     * @param entity the entity to store in the MongoDB
     * @return the new objectId of the stored entity
     */
    public ObjectId save(final Object entity) {
        return (ObjectId) this.datastore.save(entity).getId();
    }

    /**
     * @param entity the entity to remove from the MongoDB
     */
    public void delete(final Object entity) {
        this.datastore.delete(entity);
    }

    /**
     * Convert a string to a valid ObjectId. Throw an exception if the string is not a valid ObjectId, and returns the
     * corresponding ObjectId otherwise.
     *
     * @param id The String form of the desired ObjectId
     * @return The ObjectId with the provided string representation
     * @throws InvalidObjectIdException If the string is not a valid ObjectId
     */
    public static ObjectId stringToObjectId(final String id) throws InvalidObjectIdException {
        if (!ObjectId.isValid(id)) {
            throw new InvalidObjectIdException(id);
        }
        return new ObjectId(id);
    }

    /**
     * This is essentially a "login" action, in which the user obtains from the database his user information.
     *
     * @param username the user name
     * @param password the user password to check against
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

    /**
     * Get the user simply by finding his username.
     *
     * @param username the name of the user to find.
     * @return The user with the provided name or null
     */
    public User getUserByUsername(final String username) {
        final Query<User> q = this.datastore.find(User.class);
        q.criteria("username").equal(username);
        return q.get();
    }

    /**
     * This is essentially a "login" action, in which the user obtains from the database his user information.
     *
     * <i>This function is deprecated in favor of using process based token authentication</i>
     *
     * @param token The user authentication token
     * @return the user that is stored in the database that has the provided user name and password or null
     */
    @Deprecated
    public User getUserByToken(final String token) {
        final Query<User> q = this.datastore.find(User.class);
        q.criteria("authenticationToken").equal(token);
        return q.get();
    }

    /**
     * This is essentially a "login" action limited to a particular process, in which the user obtains from the database
     * his process information.
     *
     * @param token The process authentication token
     * @return the process that is stored in the database with the corresponding token
     */
    public Process getProcessByToken(final String token) {
        final Query<Process> q = this.datastore.find(Process.class);
        q.criteria("token").equal(token);
        return q.get();
    }

    /**
     * Get an unidentified node by finding its docker id
     *
     * @param dockerId the dockerId of the node to look for
     * @return The UnidentifiedNode with the provided id, or null
     */
    public UnidentifiedNode getUnidentifiedNodeByDockerId(final String dockerId) {
        final Query<UnidentifiedNode> q = this.datastore.find(UnidentifiedNode.class);
        q.criteria("dockerId").equal(dockerId);
        return q.get();
    }

    // /**
    // * Get a public node by finding its docker id
    // *
    // * @param dockerId the dockerId of the node to look for
    // * @return The PublicNode with the provided id, or null
    // */
    // public PublicNode getPublicNodeByDockerId(final String dockerId) {
    // final Query<PublicNode> q = this.datastore.find(PublicNode.class);
    // q.criteria("dockerId").equal(dockerId);
    // return q.get();
    // }

    /**
     * Retrieve the next PendingChange. It uses the findAndModify option to make sure that no tasks gets taken from the
     * queue twice.
     *
     * See http://www.programcreek.com/java-api-examples/index.php?api=com.google.code.morphia.query.UpdateOperations
     * for the polymorphism trick.
     *
     * @param lockedResources The resources that are "locked" which may not be a resource of the retrieved PendingChange
     * @return The next unobtained PendingChange, null if there are no pendingChanges
     */
    public PendingChange getNextPendingChange(final List<ObjectId> lockedResources) {
        if (lockedResources.isEmpty()) {
            return this.getNextPendingChange();
        }

        final Query<PendingChange> query = this.datastore.createQuery(PendingChange.class)
                .field("obtainedAt")
                .equal(null) // Must be null
                .field("resources")
                .hasNoneOf(lockedResources)
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

    /**
     * Clean up all pending changes that are either lingering or are in the FAILED_PERMANENTLY state, or inactive for a
     * long period of time.
     *
     * @return A piece of text containing the report of what was updated in the DB. e.g. "Deleted <i>n</i> permanently
     *         failed, and <i>m</i> lingering pending changes"
     */
    public String cleanPendingChanges() {
        // Remove pending changes that failed permanently
        final Query<PendingChange> failed = this.datastore.createQuery(PendingChange.class)
                .field("state")
                .equal(PendingChange.State.FAILED_PERMANENTLY);
        final int deletedFailed = this.datastore.delete(failed).getN();

        // Remove any pending changes that haven't been updated for a long time
        final Query<PendingChange> lingering = this.datastore.createQuery(PendingChange.class)
                .filter("obtainedAt <",
                        new Date(System.currentTimeMillis() - MongoDbConnector.PENDING_CHANGE_TIMEOUT_MS));
        final UpdateOperations<PendingChange> update = this.datastore.createUpdateOperations(PendingChange.class)
                .unset("obtainedAt");
        final int deletedLingering = this.datastore.update(lingering, update).getUpdatedCount();

        return String.format("Deleted %d permanently failed, and %d lingering pending changes",
                deletedFailed,
                deletedLingering);
    }

}
