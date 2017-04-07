package org.flexiblepower.rest;

import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.flexiblepower.api.UserApi;
import org.flexiblepower.exceptions.ApiException;
import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.InvalidObjectIdException;
import org.flexiblepower.model.User;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserRestApi extends BaseApi implements UserApi {

    private static final String USER_NOT_FOUND_MESSAGE = "User not found";

    protected UserRestApi(@Context final HttpHeaders httpHeaders, @Context final SecurityContext securityContext) {
        super(httpHeaders, securityContext);
    }

    @Override
    public String createUser(final User newUser) throws AuthorizationException {
        UserRestApi.log.debug("Received call to create new User");
        // Update the password to store it encrypted
        newUser.setPassword(newUser.getPassword());
        return this.db.insertUser(newUser);
    }

    @Override
    public String deleteUser(final String userId) throws AuthorizationException, InvalidObjectIdException {
        UserRestApi.log.debug("Received call to delete user {}", userId);
        this.db.deleteUser(userId);
        return userId;
    }

    @Override
    public User getUserById(final String userId) throws AuthorizationException, InvalidObjectIdException {
        final User ret = this.db.getUser(userId);
        if (ret == null) {
            throw new ApiException(Status.NOT_FOUND, UserRestApi.USER_NOT_FOUND_MESSAGE);
        } else {
            return ret;
        }
    }

    @Override
    public List<User> listUsers() throws AuthorizationException {
        return this.db.getUsers();
    }
}
