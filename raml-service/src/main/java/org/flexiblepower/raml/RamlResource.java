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
package org.flexiblepower.raml;

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
import java.util.regex.Pattern;

import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.flexiblepower.proto.RamlProto.RamlRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * RamlResource
 *
 * @version 0.1
 * @since Aug 20, 2019
 */
public class RamlResource {

    private static final Logger log = LoggerFactory.getLogger(RamlResource.class);
    private static final ObjectMapper om = new ObjectMapper();

    private final Object resource;
    private final Method method;
    private final String uri;

    private Pattern uriPattern;
    private String requestUri;
    private Map<String, String> queryParameters;

    RamlResource(final Object o, final Method m, final String uri) {
        this.resource = o;
        this.method = m;
        this.uri = uri;
    }

    /**
     * This is a lazy getter. That means it will deduce the URI pattern from the URI the first time it is called. After
     * that the same patter will be returned
     *
     * @return A regex pattern matching the uri. It will have any path parameters replaced with a ([^/]) group to find
     *         parameters.
     */
    public Pattern getUriPattern() {
        if (this.uriPattern == null) {
            this.uriPattern = RamlResource.getPattern(this.uri);
        }
        return this.uriPattern;
    }

    public String getUri() {
        return this.uri;
    }

    public RamlResource withRequestUri(final String uri) {
        this.requestUri = uri;
        return this;
    }

    public RamlResource withQueryParameters(final Map<String, String> params) {
        this.queryParameters = params;
        return this;
    }

    public Map<String, String> getPathParameterFromUri(final String uri) {
        return this.withRequestUri(uri).getPathParametersFromUri();
    }

    public Map<String, String> getPathParametersFromUri() {
        final Map<String, String> params = new HashMap<>();
        final Matcher keyMatcher = this.getUriPattern().matcher(this.uri);
        final Matcher valueMatcher = this.getUriPattern().matcher(this.requestUri);

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
        for (final Parameter p : this.method.getParameters()) {

            if (p.isAnnotationPresent(QueryParam.class)) {
                final QueryParam annotation = p.getAnnotation(QueryParam.class);
                if (!this.queryParameters.containsKey(annotation.value())) {
                    RamlResource.log.debug("No value found for query parameter {}, using null");
                    arguments.add(null);
                    continue;
                }
                final String param = this.queryParameters.get(annotation.value());
                arguments.add(RamlResource.valueForParameter(param, p.getType()));
            } else if (p.isAnnotationPresent(PathParam.class)) {
                final PathParam annotation = p.getAnnotation(PathParam.class);
                if (!pathParameters.containsKey(annotation.value())) {
                    RamlResource.log.debug("No value found for path parameter {}, using null");
                }
                final String param = pathParameters.get(annotation.value());
                arguments.add(RamlResource.valueForParameter(param, p.getType()));
            } else if (!usedBody) {
                usedBody = true;
                arguments.add(RamlResource.om.readValue(message.getBody(), p.getType()));
            } else {
                throw new IllegalArgumentException("Unable to get value for parameter " + p.getName()
                        + " unannotated parameters can only occur once!");
            }
        }

        return this.method.invoke(this.resource, arguments.toArray());
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
            RamlResource.log.warn("Unable to parse parameter of type {}", type);
            return null;
        }
    }

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

    /**
     * Get the Pattern for a path that we are adding. This should replace any occurences of "{X}" by a wilcard
     * fragment.
     *
     * @param path The path to create a valid pattern out of
     * @return The pattern that will match the path
     */
    static Pattern getPattern(final String path) {

        // This is the pattern to find the param groups in the path
        final Pattern param = Pattern.compile("\\{[^/\\{\\}]+\\}");
        final Matcher m = param.matcher(path);

        // Build the pattern using a stringbuilder
        int pos = 0;
        final StringBuilder pathPattern = new StringBuilder();
        while (m.find(pos)) {
            pathPattern.append(path.substring(pos, m.start()));
            pathPattern.append("([^/]+)");
            pos = m.end();
        }

        // The rest is the tail, compile and return
        pathPattern.append(path.substring(pos));
        return Pattern.compile(pathPattern.toString());
    }

}
