/**
 * File CORSResponseFilter.java
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

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

/**
 * Add a header to the response that includes the total number of items in the collection. This is relevant for pages
 * that do "pagination", i.e. request only a subset of the collection, and want to present information to the user on
 * how many items there are in the full collection.
 */
public class TotalCountFilter implements ContainerResponseFilter {

    /**
     * The name of the header that is updated by this filter, i.e. {@value #HEADER_NAME}
     */
    public static final String HEADER_NAME = "X-Total-Count";

    @Override
    public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext)
            throws IOException {
        final String count = requestContext.getHeaderString(TotalCountFilter.HEADER_NAME);
        if (count != null) {
            responseContext.getHeaders().add(TotalCountFilter.HEADER_NAME, count);
        }
    }

}