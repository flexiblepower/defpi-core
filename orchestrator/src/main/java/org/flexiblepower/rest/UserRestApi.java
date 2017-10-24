package org.flexiblepower.rest;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.bson.types.ObjectId;
import org.flexiblepower.api.UserApi;
import org.flexiblepower.connectors.MongoDbConnector;
import org.flexiblepower.exceptions.ApiException;
import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.InvalidObjectIdException;
import org.flexiblepower.model.User;
import org.flexiblepower.orchestrator.UserManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserRestApi extends BaseApi implements UserApi {

    private final UserManager db = UserManager.getInstance();

    protected UserRestApi(@Context final HttpHeaders httpHeaders) {
        super(httpHeaders);
    }

    @Override
    public User createUser(final User newUser) throws AuthorizationException {
        UserRestApi.log.debug("Received call to create new User");
        this.assertUserIsAdmin();

        // Update the password to store it encrypted
        newUser.setPasswordHash();
        newUser.setAuthenticationToken(UUID.randomUUID().toString());
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
    public User getUserByUsername(final String username) throws AuthorizationException, InvalidObjectIdException {
        final User ret = UserManager.getInstance().getUser(username);
        if (ret == null) {
            this.assertUserIsAdmin();
            throw new ApiException(Status.NOT_FOUND, UserApi.USER_NOT_FOUND_MESSAGE);
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

        if ((updatedUser.getPassword() != null) && !updatedUser.getPassword().isEmpty()) {
            updatedUser.setPassword(updatedUser.getPassword());
        }

        ret.setAdmin(updatedUser.isAdmin());
        ret.setEmail(updatedUser.getEmail());
        this.db.saveUser(ret);
        return ret;
    }

    @Override
    public Response listUsers(final int page,
            final int perPage,
            final String sortDir,
            final String sortField,
            final String filters) throws AuthorizationException {
        if (this.sessionUser == null) {
            throw new AuthorizationException();
        } else if (this.sessionUser.isAdmin()) {
            final Map<String, Object> filter = MongoDbConnector.parseFilters(filters);
            return Response.status(Status.OK.getStatusCode())
                    .header("X-Total-Count", Integer.toString(this.db.countUsers(filter)))
                    .entity(this.db.listUsers(page, perPage, sortDir, sortField, filter))
                    .build();
        } else {
            return Response.status(Status.OK.getStatusCode())
                    .header("X-Total-Count", Integer.toString(1))
                    .entity(Arrays.asList(this.sessionUser))
                    .build();
        }
    }
}
