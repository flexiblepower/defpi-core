/**
 * File BaseApi.java
 *
 * Copyright 2017 TNO
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
 * @author coenvl
 * @version 0.1
 * @since Mar 30, 2017
 */
@Slf4j
public abstract class BaseApi {

    private static final String AUTH_PREFIX = "Basic ";

    protected final User sessionUser;

    protected BaseApi(final HttpHeaders httpHeaders) {

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
        if ((pos < 1)) {
            BaseApi.log.warn("Unable to parse authentication string, not authenticated");
            this.sessionUser = null;
            return;
        }

        this.sessionUser = UserManager.getInstance().getUser(credentials.substring(0, pos),
                credentials.substring(pos + 1));
        if (this.sessionUser == null) {
            BaseApi.log.warn("Unable to find user with provided credentials");
            return;
        }

        BaseApi.log.debug("User {} logged in", this.sessionUser.getUsername());
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

}
