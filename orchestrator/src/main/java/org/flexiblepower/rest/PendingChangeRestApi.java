/**
 * File PendingChangeRestApi.java
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

import java.util.LinkedList;
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
import org.flexiblepower.model.PendingChangeDescription;
import org.flexiblepower.orchestrator.pendingchange.PendingChange;
import org.flexiblepower.orchestrator.pendingchange.PendingChangeManager;

/**
 * PendingChangeRestApi
 *
 * @version 0.1
 * @since Aug 21, 2017
 */
public class PendingChangeRestApi extends BaseApi implements PendingChangeApi {

    protected PendingChangeRestApi(@Context final HttpHeaders httpHeaders) {
        super(httpHeaders);
    }

    /**
     * This is just a conversion of the internally used object to an API object
     *
     * @param pendingChange
     * @return
     */
    private static PendingChangeDescription buildDescription(final PendingChange pendingChange) {
        return new PendingChangeDescription(pendingChange.getId(),
                pendingChange.getClass().getSimpleName(),
                pendingChange.getUserId(),
                pendingChange.getCreated(),
                pendingChange.description(),
                pendingChange.getCount(),
                pendingChange.getState().toString());
    }

    @Override
    public void deletePendingChange(final String pendingChange) throws AuthorizationException,
            InvalidObjectIdException,
            NotFoundException {
        final ObjectId pendingChangeId = MongoDbConnector.stringToObjectId(pendingChange);
        final PendingChange pc = PendingChangeManager.getInstance().getPendingChange(pendingChangeId);
        if (pc == null) {
            throw new PendingChangeNotFoundException();
        }
        this.assertUserIsAdminOrEquals(pc.getUserId());

        PendingChangeManager.getInstance().deletePendingChange(pc);
    }

    @Override
    public PendingChangeDescription getPendingChange(final String pendingChange) throws AuthorizationException,
            InvalidObjectIdException,
            NotFoundException {
        final ObjectId pendingChangeId = MongoDbConnector.stringToObjectId(pendingChange);

        final PendingChange pc = PendingChangeManager.getInstance().getPendingChange(pendingChangeId);
        if (pc == null) {
            throw new PendingChangeNotFoundException();
        }
        this.assertUserIsAdminOrEquals(pc.getUserId());

        return PendingChangeRestApi.buildDescription(pc);
    }

    @Override
    public Response listPendingChanges(final int page,
            final int perPage,
            final String sortDir,
            final String sortField,
            final String filters) throws AuthorizationException {
        if (this.sessionUser == null) {
            throw new AuthorizationException();
        }

        final Map<String, Object> filter = MongoDbConnector.parseFilters(filters);
        if (!this.sessionUser.isAdmin()) {
            // When not admin, filter for this specific user
            filter.put("userId", this.sessionUser.getId());
        }

        final List<PendingChange> list = PendingChangeManager.getInstance()
                .listPendingChanges(page, perPage, sortDir, sortField, filter);

        final List<PendingChangeDescription> realList = new LinkedList<>();
        list.forEach((pcd) -> realList.add(PendingChangeRestApi.buildDescription(pcd)));

        return Response.status(Status.OK.getStatusCode())
                .header("X-Total-Count",
                        Integer.toString(PendingChangeManager.getInstance().countPendingChanges(filter)))
                .entity(realList)
                .build();
    }

    @Override
    public void cleanPendingChanges() throws AuthorizationException {
        this.assertUserIsAdmin();
        PendingChangeManager.getInstance().cleanPendingChanges();
    }

}
