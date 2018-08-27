/*-
 * #%L
 * dEF-Pi REST Orchestrator
 * %%
 * Copyright (C) 2017 - 2018 Flexible Power Alliance Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.flexiblepower.rest;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

import org.flexiblepower.api.ServiceApi;
import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.Service;
import org.flexiblepower.orchestrator.ServiceManager;

/**
 * ServiceRestApi
 *
 * @version 0.1
 * @since Mar 30, 2017
 */
public class ServiceRestApi extends BaseApi implements ServiceApi {

    private static final Map<String, Comparator<Service>> SORT_MAP = new HashMap<>();
    static {
        ServiceRestApi.SORT_MAP.put("default", Comparator.comparing(Service::getId));
        ServiceRestApi.SORT_MAP.put("id", Comparator.comparing(Service::getId));
        ServiceRestApi.SORT_MAP.put("name", Comparator.comparing(Service::getName));
        ServiceRestApi.SORT_MAP.put("created", Comparator.comparing(Service::getCreated));
        ServiceRestApi.SORT_MAP.put("version", Comparator.comparing(Service::getVersion));
    }

    /**
     * Create the REST API with the headers from the HTTP request (will be injected by the HTTP server)
     *
     * @param httpHeaders The headers from the HTTP request for authorization
     */
    protected ServiceRestApi(@Context final HttpHeaders httpHeaders) {
        super(httpHeaders);
    }

    @Override
    public List<Service> listServices(final int page,
            final int perPage,
            final String sortDir,
            final String sortField,
            final String filters) throws AuthorizationException {
        this.assertUserIsLoggedIn();

        final List<Service> content = ServiceManager.getInstance().listServices();
        RestUtils.orderContent(content, ServiceRestApi.SORT_MAP, sortField, sortDir);

        this.addTotalCount(content.size());
        return RestUtils.paginate(content, page, perPage);
    }

    @Override
    public Service getService(final String id) throws ServiceNotFoundException, AuthorizationException {
        this.assertUserIsLoggedIn();
        return ServiceManager.getInstance().getService(id);
    }

    @Override
    public List<Service> getAllServiceVersions(final String id) throws ServiceNotFoundException,
            AuthorizationException {
        // Should we paginate even this?
        this.assertUserIsLoggedIn();
        final List<Service> content = ServiceManager.getInstance().listServiceVersions(id);
        this.addTotalCount(content.size());
        return content;
    }

}
