/*-
 * #%L
 * dEF-Pi API
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
package org.flexiblepower.raml.server;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;

import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.flexiblepower.proto.RamlProto.RamlRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * RamlResourceRequest
 *
 * @version 0.1
 * @since Aug 22, 2019
 */
public class RamlResourceRequest {

    private static final ObjectMapper om = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(RamlResourceRequest.class);

    private final RamlResource resource;
    private final String requestUri;
    private final Map<String, String> queryParameters;

    /**
     * @param r The RamlResource that we are requesting
     * @param uri The requested URI containing the path parameter values
     * @param params A map of query parameters
     */
    RamlResourceRequest(final RamlResource r, final String uri, final Map<String, String> params) {
        this.resource = r;
        this.requestUri = uri;
        this.queryParameters = params;
    }

    /**
     * Get path parameters from the URI of this resource
     *
     * @return a key value map with path parameters
     */
    public Map<String, String> getPathParametersFromUri() {
        final Map<String, String> params = new HashMap<>();
        final Matcher keyMatcher = this.resource.getUriPattern().matcher(this.resource.getUri());
        final Matcher valueMatcher = this.resource.getUriPattern().matcher(this.requestUri);

        if (keyMatcher.matches() && valueMatcher.matches() && (keyMatcher.groupCount() == valueMatcher.groupCount())) {
            for (int i = 1; i <= keyMatcher.groupCount(); i++) {
                final String key = keyMatcher.group(i);
                params.put(key.substring(1, key.length() - 1), valueMatcher.group(i));
            }
        }

        return params;
    }

    /**
     * @param message The RAML message to invoke the resource for
     * @return the result of dispatching the method represented by this object on obj with parameters args
     * @throws InvocationTargetException if the underlying method throws an exception
     * @throws IllegalArgumentException if the method is an instance method and the specified object argument is not an
     *             instance of the class or interface declaring the underlying method (or of a subclass or implementor
     *             thereof); if the number of actual and formal parameters differ; if an unwrapping conversion for
     *             primitive arguments fails; or if, after possible unwrapping, a parameter value cannot be converted to
     *             the corresponding formal parameter type by a method invocation conversion.
     * @throws IllegalAccessException if this Method object is enforcing Java language access control and the underlying
     *             method is inaccessible.
     * @throws JsonProcessingException if the parameters are not valid JSON
     * @throws JsonMappingException if the parameters structure does not match structure expected for result type (or
     *             has other mismatch issues)
     * @see Method#invoke(Object, Object...)
     */
    public Object invoke(final RamlRequest message) throws IllegalAccessException,
            IllegalArgumentException,
            InvocationTargetException,
            JsonMappingException,
            JsonProcessingException {
        final List<Object> arguments = new ArrayList<>();
        final Map<String, String> pathParameters = this.getPathParametersFromUri();
        boolean usedBody = false;
        for (final Parameter p : this.resource.getParameters()) {

            if (p.isAnnotationPresent(QueryParam.class)) {
                final QueryParam annotation = p.getAnnotation(QueryParam.class);
                if (!this.queryParameters.containsKey(annotation.value())) {
                    RamlResourceRequest.log.debug("No value found for query parameter {}, using null");
                    arguments.add(null);
                    continue;
                }
                final String param = this.queryParameters.get(annotation.value());
                arguments.add(RamlResourceRequest.valueForParameter(param, p.getType()));
            } else if (p.isAnnotationPresent(PathParam.class)) {
                final PathParam annotation = p.getAnnotation(PathParam.class);
                if (!pathParameters.containsKey(annotation.value())) {
                    RamlResourceRequest.log.debug("No value found for path parameter {}, using null");
                }
                final String param = pathParameters.get(annotation.value());
                arguments.add(RamlResourceRequest.valueForParameter(param, p.getType()));
            } else if (!usedBody) {
                usedBody = true;
                arguments.add(RamlResourceRequest.om.readValue(message.getBody(), p.getType()));
            } else {
                throw new IllegalArgumentException("Unable to get value for parameter " + p.getName()
                        + " unannotated parameters can only occur once!");
            }
        }

        return this.resource.invoke(arguments.toArray());
    }

    /**
     * Get query parameters and their values from a URI
     *
     * @param uri The URI with query parameters encoded
     * @return a key value map with query parameters
     */
    public static Map<String, String> getQueryParametersFromUri(final String uri) {
        final int questionMark = uri.indexOf('?');
        if (questionMark == -1) {
            return Collections.emptyMap();
        }
        final String params = uri.substring(questionMark + 1);
        final Map<String, String> map = new TreeMap<>();
        for (final String keyvalue : params.split("&")) {
            final int equals = keyvalue.indexOf('=');
            if (equals == -1) {
                map.put(keyvalue, "");
            } else {
                map.put(keyvalue.substring(0, equals), keyvalue.substring(equals + 1));
            }
        }
        return map;
    }

    private static Object valueForParameter(final String param, final Class<?> type) {
        if (type.equals(Byte.class) || type.equals(byte.class)) {
            return Byte.parseByte(param);
        } else if (type.equals(Short.class) || type.equals(short.class)) {
            return Short.parseShort(param);
        } else if (type.equals(Integer.class) || type.equals(int.class)) {
            return Integer.parseInt(param);
        } else if (type.equals(Long.class) || type.equals(long.class)) {
            return Long.parseLong(param);
        } else if (type.equals(Float.class) || type.equals(float.class)) {
            return Float.parseFloat(param);
        } else if (type.equals(Double.class) || type.equals(double.class)) {
            return Double.parseDouble(param);
        } else if (type.equals(String.class)) {
            return param;
        } else {
            RamlResourceRequest.log.warn("Unable to parse parameter of type {}", type);
            return null;
        }
    }

}
