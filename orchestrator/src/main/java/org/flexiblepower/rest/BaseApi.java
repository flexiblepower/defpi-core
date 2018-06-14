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
import org.flexiblepower.model.Process;
import org.flexiblepower.model.User;
import org.flexiblepower.orchestrator.UserManager;
import org.flexiblepower.process.ProcessManager;

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

    /**
     * The session user is the user who successfully logged in using Basic authentication or using his authentication
     * token.
     */
    protected final User sessionUser;

    /**
     * The HTTP headers of the session, which may include headers to add to the response by any filter
     */
    protected final HttpHeaders headers;

    /**
     * Create the abstract base class for the REST API. The base API will make sure the user is logged in by taking the
     * authentication headers from the HTTP request.
     *
     * @param httpHeaders The headers of the HTTP request that creates this object
     */
    protected BaseApi(final HttpHeaders httpHeaders) {
        this.headers = httpHeaders;
        String authString = httpHeaders.getHeaderString("Authorization");
        if ((authString == null) || !authString.startsWith(BaseApi.AUTH_PREFIX)) {
            BaseApi.log.warn("Client is not using basic authentication, not authenticated");
            this.sessionUser = null;
            return;
        }

        // Trim the prefix
        authString = authString.substring(BaseApi.AUTH_PREFIX.length());
        final String credentials = new String(Base64.getDecoder().decode(authString));
        final int pos = credentials.indexOf(':');
        if (pos < 1) {
            BaseApi.log.debug("Unable to parse authentication string, not authenticated");
            this.sessionUser = null;
            return;
        }

        this.sessionUser = UserManager.getInstance()
                .getUser(credentials.substring(0, pos), credentials.substring(pos + 1));
        if (this.sessionUser == null) {
            BaseApi.log.debug("Unable to find user with provided credentials");
            return;
        }
        // Success!
        BaseApi.log.trace("User {} logged in", this.sessionUser.getUsername());
    }

    /**
     * Get the process that belongs to the login action. This will only return a process if the client provided a
     * X-Auth-Token header with a token that was generated while creating a new process.
     *
     * @return The process that the token refers to, or null if no such process exists
     */
    protected Process getTokenProcess() {
        // User did not provide Basic Auth info, so look for token in header
        final String token = this.headers.getHeaderString("X-Auth-Token");
        if (token == null) {
            BaseApi.log.debug("Client is not using token-based authentication, no authenticated process!");
            return null;
        }

        // If token is present, try to get a process that matches the token
        final Process ret = ProcessManager.getInstance().getProcessByToken(token);
        if (ret == null) { // If no match found, no user found
            BaseApi.log.debug("Unable to find process with provided token");
        }
        BaseApi.log.trace("Process {} logged in", ret.getId());
        return ret;
    }

    /**
     * Protected function that only throws an exception if the current logged in user is not an admin.
     *
     * @throws AuthorizationException If the assertion fails
     */
    protected void assertUserIsAdmin() throws AuthorizationException {
        if ((this.sessionUser == null) || !this.sessionUser.isAdmin()) {
            throw new AuthorizationException();
        }
    }

    /**
     * Protected function that throws an exception if there is no logged in user.
     *
     * @throws AuthorizationException If the assertion fails
     */
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
     * @throws AuthorizationException If the assertion fails
     */
    protected void assertUserIsAdminOrEquals(final ObjectId userId) throws AuthorizationException {
        if (this.sessionUser == null) {
            throw new AuthorizationException();
        }
        if (!this.sessionUser.isAdmin() && !this.sessionUser.getId().equals(userId)) {
            throw new AuthorizationException();
        }

    }

    /**
     * Add the {@value TotalCountFilter#HEADER_NAME} header to the response with the provided value
     *
     * @param count The total number of responses that could have been returned
     */
    protected void addTotalCount(final int count) {
        this.headers.getRequestHeaders().add(TotalCountFilter.HEADER_NAME, Integer.toString(count));
    }

}
