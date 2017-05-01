/**
 * File BaseApi.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.rest;

import java.util.Base64;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;

import org.flexiblepower.exceptions.ApiException;
import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.model.User;
import org.flexiblepower.orchestrator.MongoDbConnector;

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

    @Deprecated // Alleen toegang via managers, niet direct!
    protected final MongoDbConnector db = new MongoDbConnector();

    protected final User loggedInUser;

    protected BaseApi(final HttpHeaders httpHeaders, final SecurityContext securityContext) {
        final String authPrefix = "Basic ";

        String authString = httpHeaders.getHeaderString("Authorization");
        if ((authString == null) || !authString.startsWith(authPrefix)) {
            BaseApi.log.warn("Client is not using basic authentication, not authenticated");
            this.loggedInUser = null;
            return;
        }

        // Trim the prefix
        authString = authString.substring(authPrefix.length());
        final String[] credentials = new String(Base64.getDecoder().decode(authString)).split(":");
        if ((credentials.length != 2)) {
            BaseApi.log.warn("Unable to parse authentication string, not authenticated");
            this.loggedInUser = null;
            return;
        }

        this.loggedInUser = this.db.getUser(credentials[0], credentials[1]);
        this.db.setApplicationUser(this.loggedInUser);
        BaseApi.log.debug("User {} logged in", credentials[0]);
    }

    /**
     * Protected function that only throws an exception if the current logged in user is not an admin.
     *
     * @throws AuthorizationException
     */
    protected void assertUserIsAdmin() throws ApiException {
        if ((this.loggedInUser == null) || !this.loggedInUser.isAdmin()) {
            throw new ApiException(new AuthorizationException());
        }
    }

}
