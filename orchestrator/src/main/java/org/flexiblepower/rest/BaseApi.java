/**
 * File BaseApi.java
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

import java.util.Base64;

import javax.ws.rs.core.HttpHeaders;

import org.bson.types.ObjectId;
import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.model.User;
import org.flexiblepower.orchestrator.UserManager;

import lombok.extern.slf4j.Slf4j;

/**
 * BaseApi
 *
 * @version 0.1
 * @since Mar 30, 2017
 */
@Slf4j
public abstract class BaseApi {

    private static final String AUTH_PREFIX = "Basic ";
    protected final HttpHeaders headers;
    protected final User sessionUser;

    protected BaseApi(final HttpHeaders httpHeaders) {
        this.headers = httpHeaders;
        String authString = httpHeaders.getHeaderString("Authorization");
        if ((authString == null) || !authString.startsWith(BaseApi.AUTH_PREFIX)) {
            // User did not provide Basic Auth info, so look for token in header
            BaseApi.log.trace("Client is not using basic authentication! Looking for token header");
            final String token = httpHeaders.getHeaderString("X-Auth-Token");
            if (token == null) { // If token is also not present, user is unauthenticated!
                BaseApi.log.debug("Client is not using token-based authentication either! Unauthenticated!");
                this.sessionUser = null;
                return;
            }
            // If token is present, try to get a user that matches the token
            this.sessionUser = UserManager.getInstance().getUserByToken(token);
            if (this.sessionUser == null) { // If no match found, no user found
                BaseApi.log.debug("Unable to find user with provided token");
                return;
            }
        } else {
            // Trim the prefix
            authString = authString.substring(BaseApi.AUTH_PREFIX.length());
            final String credentials = new String(Base64.getDecoder().decode(authString));
            final int pos = credentials.indexOf(':');
            if ((pos < 1)) {
                BaseApi.log.debug("Unable to parse authentication string, not authenticated");
                this.sessionUser = null;
                return;
            }

            this.sessionUser = UserManager.getInstance().getUser(credentials.substring(0, pos),
                    credentials.substring(pos + 1));
            if (this.sessionUser == null) {
                BaseApi.log.debug("Unable to find user with provided credentials");
                return;
            }
        }
        // Success!
        BaseApi.log.trace("User {} logged in", this.sessionUser.getUsername());
    }

    /**
     * Protected function that only throws an exception if the current logged in user is not an admin.
     *
     * @throws AuthorizationException
     */
    protected void assertUserIsAdmin() throws AuthorizationException {
        if ((this.sessionUser == null) || !this.sessionUser.isAdmin()) {
            throw new AuthorizationException();
        }
    }

    protected void assertUserIsLoggedIn() throws AuthorizationException {
        if (this.sessionUser == null) {
            throw new AuthorizationException();
        }
    }

    /**
     * Protected function that only throws an exception if the current logged in user is not an admin or equals the
     * provided userId.
     *
     * @param userId The userId that should be logged in
     * @throws AuthorizationException
     */
    protected void assertUserIsAdminOrEquals(final ObjectId userId) throws AuthorizationException {
        if (this.sessionUser == null) {
            throw new AuthorizationException();
        }
        if (!this.sessionUser.isAdmin() && !this.sessionUser.getId().equals(userId)) {
            throw new AuthorizationException();
        }

    }

    protected void addTotalCount(final int count) {
        this.headers.getRequestHeaders().add(TotalCountFilter.HEADER_NAME, Integer.toString(count));
    }

}
