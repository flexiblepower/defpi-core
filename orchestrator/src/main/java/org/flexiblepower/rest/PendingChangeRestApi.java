/**
 * File PendingChangeRestApi.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.bson.types.ObjectId;
import org.flexiblepower.api.PendingChangeApi;
import org.flexiblepower.connectors.MongoDbConnector;
import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.InvalidObjectIdException;
import org.flexiblepower.exceptions.NotFoundException;
import org.flexiblepower.exceptions.PendingChangeNotFoundException;
import org.flexiblepower.orchestrator.pendingchange.PendingChange;
import org.flexiblepower.orchestrator.pendingchange.PendingChangeManager;

/**
 * PendingChangeRestApi
 *
 * @author wilco
 * @version 0.1
 * @since Aug 21, 2017
 */
public class PendingChangeRestApi extends BaseApi implements PendingChangeApi {

    protected PendingChangeRestApi(@Context final HttpHeaders httpHeaders) {
        super(httpHeaders);
    }

    @Override
    public void deletePendingChange(final String pendingChange)
            throws AuthorizationException, InvalidObjectIdException, NotFoundException {
        if (this.sessionUser == null) {
            throw new AuthorizationException();
        } else {
            final ObjectId pendingChangeId = MongoDbConnector.stringToObjectId(pendingChange);
            final PendingChange pc = PendingChangeManager.getInstance().getPendingChange(pendingChangeId);
            if (pc == null) {
                throw new PendingChangeNotFoundException();
            }
            if (!this.sessionUser.isAdmin()) {
                if (!this.sessionUser.getId().equals(pc.getUserId())) {
                    throw new AuthorizationException();
                }
            }
            PendingChangeManager.getInstance().deletePendingChange(pendingChangeId);
        }
    }

    @Override
    public PendingChangeModel getPendingChange(final String pendingChange)
            throws AuthorizationException, InvalidObjectIdException, NotFoundException {
        if (this.sessionUser == null) {
            throw new AuthorizationException();
        } else {
            final ObjectId pendingChangeId = MongoDbConnector.stringToObjectId(pendingChange);
            final PendingChange pc = PendingChangeManager.getInstance().getPendingChange(pendingChangeId);
            if (pc == null) {
                throw new PendingChangeNotFoundException();
            }
            if (!this.sessionUser.isAdmin()) {
                if (!this.sessionUser.getId().equals(pc.getUserId())) {
                    throw new AuthorizationException();
                }
            }
            return new PendingChangeModel(pc);
        }
    }

    @Override
    public Response listPendingChanges(final int page,
            final int perPage,
            final String sortDir,
            final String sortField,
            final String filters) throws AuthorizationException {
        if (this.sessionUser == null) {
            throw new AuthorizationException();
        } else {
            final Map<String, Object> filter = MongoDbConnector.parseFilters(filters);
            if (!this.sessionUser.isAdmin()) {
                // When not admin, filter for this specific user
                filter.put("userId", this.sessionUser.getId());
            }
            final List<PendingChange> list = MongoDbConnector.getInstance().list(PendingChange.class,
                    page,
                    perPage,
                    sortDir,
                    sortField,
                    filter);
            final List<PendingChangeModel> realList = new ArrayList<>(list.size());
            for (final PendingChange pc : list) {
                realList.add(new PendingChangeModel(pc));
            }
            return Response.status(Status.OK.getStatusCode())
                    .header("X-Total-Count",
                            Integer.toString(MongoDbConnector.getInstance().totalCount(PendingChange.class, filter)))
                    .entity(realList)
                    .build();
        }
    }

}
