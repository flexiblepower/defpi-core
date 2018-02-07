/**
 * File UserManager.java
 *
 * Copyright 2017 FAN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flexiblepower.orchestrator;

import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.flexiblepower.connectors.MongoDbConnector;
import org.flexiblepower.model.User;

/**
 * UserManager
 *
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
     * @param userId the Id of the user
     * @return the user stored with the provided Id, or null
     */
    public User getUser(final ObjectId userId) {
        return this.db.get(User.class, userId);
    }

    /**
     * Get the user simply by his username. Caution this function should only be possible from an administrator account
     *
     * FIXME This seems to be a security leak isn't it?
     *
     * @param username the name of the user to find.
     * @return The user with the provided name or null
     */
    public User getUser(final String username) {
        return this.db.getUserByUsername(username);
    }

    /**
     * This is essentially a "login" action, in which the user obtains from the database his user information.
     *
     * @param username the user name
     * @param password the user password to check against
     * @return the user that is stored in the database that has the provided user name and password
     */
    public User getUser(final String username, final String password) {
        return this.db.getUser(username, password);
    }

    /**
     * This is essentially a "login" action, in which the user obtains from the database his user information.
     *
     * @param token The user authentication token
     * @return the user that is stored in the database that has the provided user name and password or null
     */
    public User getUserByToken(final String token) {
        return this.db.getUserByToken(token);
    }

    /**
     * Inserts a user object in the database.
     *
     * @param newUser the new user to insert into the database
     * @return the userId of the inserted user
     */
    public ObjectId saveUser(final User newUser) {
        return this.db.save(newUser);
    }

    /**
     * Deletes a user object from the database that has the provided userId.
     *
     * @param user the user to remove from the database
     */
    public void deleteUser(final User user) {
        this.db.delete(user);
    }

    /**
     * Count all users currently stored in the database
     *
     * @param filter A filter to count a specific filtered subset of users, may be empty
     * @return The number of users that match the filter
     */
    public int countUsers(final Map<String, Object> filter) {
        return this.db.totalCount(User.class, filter);
    }

    public List<User> listUsers(final int page,
            final int perPage,
            final String sortDir,
            final String sortField,
            final Map<String, Object> filter) {
        return this.db.list(User.class, page, perPage, sortDir, sortField, filter);
    }

}
