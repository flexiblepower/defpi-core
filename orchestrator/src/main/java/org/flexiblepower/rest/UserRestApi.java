/**
 * File UserRestApi.java
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

package org.flexiblepower.rest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;

import org.bson.types.ObjectId;
import org.flexiblepower.api.UserApi;
import org.flexiblepower.connectors.MongoDbConnector;
import org.flexiblepower.exceptions.*;
import org.flexiblepower.model.User;
import org.flexiblepower.orchestrator.UserManager;

import lombok.extern.slf4j.Slf4j;

/**
 * UserRestApi
 *
 * @version 0.1
 * @since Mar 30, 2017
 */
@Slf4j
public class UserRestApi extends BaseApi implements UserApi {

    private final UserManager db = UserManager.getInstance();

    /**
     * Create the REST API with the headers from the HTTP request (will be injected by the HTTP server)
     *
     * @param httpHeaders The headers from the HTTP request for authorization
     */
    protected UserRestApi(@Context final HttpHeaders httpHeaders) {
        super(httpHeaders);
    }

    @Override
    public User createUser(final User newUser) throws AuthorizationException {
        UserRestApi.log.debug("Received call to create new User");
        this.assertUserIsAdmin();

        // Update the password to store it encrypted
        if ((newUser.getPassword() != null) && (newUser.getPasswordHash() == null)) {
            newUser.setPasswordHash();
        }

        this.db.saveUser(newUser);
        return newUser;
    }

    @Override
    public void deleteUser(final String userId) throws AuthorizationException, InvalidObjectIdException {
        UserRestApi.log.debug("Received call to delete user {}", userId);
        this.assertUserIsAdmin();

        this.db.deleteUser(this.db.getUser(MongoDbConnector.stringToObjectId(userId)));
    }

    @Override
    public User getUser(final String userId) throws AuthorizationException, InvalidObjectIdException {
        final ObjectId oid = MongoDbConnector.stringToObjectId(userId);
        this.assertUserIsAdminOrEquals(oid);

        final User ret = this.db.getUser(oid);
        if (ret == null) {
            throw new ApiException(Status.NOT_FOUND, UserApi.USER_NOT_FOUND_MESSAGE);
        } else {
            return ret;
        }
    }

    @Override
    public User getUserByUsername(final String username) throws AuthorizationException, UserNotFoundException {
        final User ret = UserManager.getInstance().getUser(username);
        if (ret == null) {
            this.assertUserIsAdmin();
            throw new UserNotFoundException(username);
        } else {
            this.assertUserIsAdminOrEquals(ret.getId());
            return ret;
        }
    }

    @Override
    public User updateUser(final String userId, final User updatedUser) throws AuthorizationException {
        UserRestApi.log.debug("Received call to update user");
        if ((updatedUser.getId() == null) || (userId == null) || !userId.equals(updatedUser.getId().toString())) {
            throw new ApiException(Status.BAD_REQUEST, "No or wrong id provided");
        }

        this.assertUserIsAdminOrEquals(updatedUser.getId());

        final User ret = this.db.getUser(updatedUser.getId());
        if (ret == null) {
            throw new ApiException(Status.NOT_FOUND, UserApi.USER_NOT_FOUND_MESSAGE);
        }

        if (!ret.getUsername().equals(updatedUser.getUsername())) {
            throw new ApiException(Status.BAD_REQUEST, "Name cannot be changed");
        }

        // if ((updatedUser.getPassword() != null) && !updatedUser.getPassword().isEmpty()) {
        // ret.setPassword(updatedUser.getPassword());
        // }
        updatedUser.setPasswordHash();
        if ((updatedUser.getPasswordHash() != null) && !updatedUser.getPasswordHash().isEmpty()) {
            ret.setPasswordHash(updatedUser.getPasswordHash());
        }

        ret.setAdmin(updatedUser.isAdmin());
        ret.setEmail(updatedUser.getEmail());
        this.db.saveUser(ret);
        return ret;
    }

    @Override
    public List<User> listUsers(final int page,
            final int perPage,
            final String sortDir,
            final String sortField,
            final String filters) throws AuthorizationException {
        if (this.sessionUser == null) {
            throw new AuthorizationException();
        } else if (this.sessionUser.isAdmin()) {
            final Map<String, Object> filter = MongoDbConnector.parseFilters(filters);

            @SuppressWarnings("unchecked")
            final List<User> userList = filter.containsKey("ids[]")
                    ? this.db.getUsers((List<String>) filter.get("ids[]"))
                    : this.db.listUsers(page, perPage, sortDir, sortField, filter);

            userList.forEach((u) -> u.clearPasswordHash());

            this.addTotalCount(this.db.countUsers(filter));
            return userList;
        } else {
            final User ret = this.sessionUser;
            ret.clearPasswordHash();
            this.addTotalCount(1);
            return Arrays.asList(ret);
        }
    }
}
