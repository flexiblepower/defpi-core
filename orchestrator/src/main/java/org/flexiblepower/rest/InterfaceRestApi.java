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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

import org.flexiblepower.api.InterfaceApi;
import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.model.Interface;
import org.flexiblepower.orchestrator.ServiceManager;

/**
 * InterfaceRestApi
 *
 * @version 0.1
 * @since Mar 30, 2017
 */
public class InterfaceRestApi extends BaseApi implements InterfaceApi {

    private static final Map<String, Comparator<Interface>> SORT_MAP = new HashMap<>();
    static {
        InterfaceRestApi.SORT_MAP.put("default", Comparator.comparing(Interface::getId));
        InterfaceRestApi.SORT_MAP.put("id", Comparator.comparing(Interface::getId));
        InterfaceRestApi.SORT_MAP.put("serviceId", Comparator.comparing(Interface::getServiceId));
        InterfaceRestApi.SORT_MAP.put("name", Comparator.comparing(Interface::getName));
    }

    /**
     * Create the REST API with the headers from the HTTP request (will be injected by the HTTP server)
     *
     * @param httpHeaders The headers from the HTTP request for authorization
     */
    protected InterfaceRestApi(@Context final HttpHeaders httpHeaders) {
        super(httpHeaders);
    }

    @Override
    public List<Interface> listInterfaces(final int page,
            final int perPage,
            final String sortDir,
            final String sortField,
            final String filters) throws AuthorizationException {
        // TODO implement pagination, sorting and filtering
        this.assertUserIsLoggedIn();
        if ((page < 0) || (perPage < 0)) {
            this.addTotalCount(0);
            return Collections.emptyList();
        }

        final List<Interface> content = ServiceManager.getInstance().listInterfaces();
        RestUtils.orderContent(content, InterfaceRestApi.SORT_MAP, sortField, sortDir);

        this.addTotalCount(content.size());
        return RestUtils.paginate(content, page, perPage);
    }

    @Override
    public Interface getInterface(final String id) throws AuthorizationException {
        this.assertUserIsLoggedIn();
        return ServiceManager.getInstance().getInterfaceById(id);
    }

}
