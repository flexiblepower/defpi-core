/**
 * File UserManager.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator;

import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.InvalidObjectIdException;
import org.flexiblepower.model.User;

/**
 * UserManager
 *
 * @author coenvl
 * @version 0.1
 * @since Jul 24, 2017
 */
public class UserManager {

    private static UserManager instance;

    private final MongoDbConnector db = MongoDbConnector.getInstance();

    private UserManager() {
        // Private constructor
    }

    public synchronized static UserManager getInstance() {
        if (UserManager.instance == null) {
            UserManager.instance = new UserManager();
        }
        return UserManager.instance;
    }

    /**
     * @return a list of all users currently stored in the mongo db.
     */
    public List<User> getUsers() {
        return this.db.list(User.class);
    }

    /**
     * Get a user object from the database that has the provided userId, or null if no such user exists.
     *
     * @param userId
     * @return the user stored with the provided Id, or null
     */
    public User getUser(final ObjectId userId) {
        return this.db.get(User.class, userId);
    }

    /**
     * This is essentially a "login" action, in which the user obtains from the database his user information.
     *
     * @param username
     * @param password
     * @return the user that is stored in the database that has the provided user name and password
     */
    public User getUser(final String username, final String password) {
        return this.db.getUser(username, password);
    }

    /**
     * This function creates a new user with the provided name and password. This is essentially a "registration"
     * function.
     *
     * @param username
     * @param password
     * @return the userId of the newly created user
     */
    public ObjectId createNewUser(final String username, final String password) {
        return this.db.save(new User(username, password));
    }

    /**
     * Inserts a user object in the database.
     *
     * @param user
     * @param newUser
     * @return the userId of the inserted user
     */
    public ObjectId saveUser(final User newUser) {
        return this.db.save(newUser);
    }

    /**
     * Deletes a user object from the database that has the provided userId.
     *
     * @param userId
     * @throws AuthorizationException
     * @throws InvalidObjectIdException
     */
    public void deleteUser(final User user) {
        this.db.delete(user);
    }

    public int countUsers(final Map<String, Object> filter) {
        return this.db.totalCount(User.class, filter);
    }

    public Object listUsers(final int page,
            final int perPage,
            final String sortDir,
            final String sortField,
            final Map<String, Object> filter) {
        return this.db.list(User.class, page, perPage, sortDir, sortField, filter);
    }

}
