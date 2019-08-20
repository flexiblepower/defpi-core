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
package org.flexiblepower.raml;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.flexiblepower.proto.RamlProto.RamlRequest;
import org.flexiblepower.service.ConnectionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RamlResourceRegistry
 *
 * @version 0.1
 * @since Aug 10, 2019
 */
public class RamlResourceRegistry {

    protected static final Logger log = LoggerFactory.getLogger(RamlRequestHandler.class);

    /**
     * Entries in this map have as the key, their top level path (i.e. the path the class is annotated with), and as
     * their entry a MethodRegistry.
     */
    private final Map<String, MethodRegistry> resources = new HashMap<>();

    /**
     * Create a RAML resource registry for the provided handler
     *
     * @param handler The connection handler to create a registry for.
     */
    public RamlResourceRegistry(final ConnectionHandler handler) {
        for (final Method m : handler.getClass().getMethods()) {
            final Class<?> clazz = m.getReturnType();
            if (m.getName().equals("get" + clazz.getSimpleName()) && (m.getParameters().length == 0)
                    && clazz.isAnnotationPresent(Path.class)) {
                // This is a resource provider
                try {
                    final Object resource = m.invoke(handler);
                    this.resources.put(clazz.getAnnotation(Path.class).value(), new MethodRegistry(resource));
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    RamlResourceRegistry.log.warn("Unable to add resource {} for interface {}",
                            clazz.getSimpleName(),
                            handler.getClass());
                }
            }
        }
    }

    /**
     * @param message A RAML message for which we want to get the resource and method depending on the URI and the
     *            method. (In the future possibly also the headers)
     * @return The RAML resource that this message should invoke
     */
    public RamlResource getResourceForMessage(final RamlRequest message) {
        String uri = message.getUri();
        final Map<String, String> queryParams = RamlResourceRegistry.getQueryParametersFromUri(uri);
        final int questionMark = uri.indexOf('?');
        if (questionMark != -1) {
            uri = uri.substring(0, questionMark);
        }

        final int delim = uri.indexOf('/', 1);
        final String top = uri.substring(0, delim > 0 ? delim : uri.length());

        if (!this.resources.containsKey(top)) {
            RamlResourceRegistry.log.warn("Unable to locate resource at path " + top);
            return null;
        }

        final String suffixFromQueryParams = queryParams.isEmpty() ? "" : "?" + String.join("&", queryParams.keySet());
        final String key = message.getMethod().name() + uri.substring(top.length()) + suffixFromQueryParams;

        return this.resources.get(top).getResource(key).withURI(uri).withQueryParameters(queryParams);
    }

    private static Map<String, String> getQueryParametersFromUri(final String uri) {
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
     * MethodRegistry
     *
     * @version 0.1
     * @since Aug 10, 2019
     */
    static class MethodRegistry {

        private final Map<String, RamlResource> fixedPaths = new HashMap<>();
        private final Map<Pattern, RamlResource> wildcardPaths = new HashMap<>();

        /**
         * Create a MethodRegistry for a resource. That is find any methods that correspond to a REST endpoint and parse
         * them already to reduce processing later on.
         *
         * @param resource The resource to build the registry of
         */
        MethodRegistry(final Object resource) {
            Class<?> clazz = resource.getClass();
            if (!clazz.isAnnotationPresent(Path.class)) {
                for (final Class<?> itf : clazz.getInterfaces()) {
                    if (itf.isAnnotationPresent(Path.class)) {
                        clazz = itf;
                        break;
                    }
                }
            }
            if (!clazz.isAnnotationPresent(Path.class)) {
                RamlResourceRegistry.log.error("Unable to determine the Path annotated superclass of {}",
                        resource.getClass());
            }

            for (final Method m : clazz.getMethods()) {
                // Here I assume there is only one restMethod per method.
                final String restMethod = MethodRegistry.restMethodOf(m);
                if (restMethod == null) {
                    continue;
                }
                final String queryParams = MethodRegistry.getQueryParametersFromMethod(m);

                if (!m.isAnnotationPresent(Path.class)) {
                    // This is a simple method, no extra path
                    this.fixedPaths.put(restMethod + queryParams, new RamlResource(resource, m));
                    RamlResourceRegistry.log.debug("Added \"{}\" for resource path {}",
                            restMethod,
                            resource.getClass().getSimpleName());
                    continue;
                }

                final String typePath = m.getAnnotation(Path.class).value();
                if (!typePath.contains("{")) {
                    // This is a method with some additional path
                    this.fixedPaths.put(restMethod + typePath + queryParams, new RamlResource(resource, m));
                    RamlResourceRegistry.log.debug("Added \"{}\" for resource path {}",
                            restMethod + typePath,
                            resource.getClass().getSimpleName());
                } else {
                    // This method has a path with a parameter in it
                    final Pattern uriPattern = MethodRegistry.getPattern(restMethod + typePath + queryParams);
                    this.wildcardPaths.put(uriPattern, new RamlResource(resource, m, uriPattern));
                    RamlResourceRegistry.log.debug("Added \"{}\" for resource path {}",
                            restMethod + typePath,
                            resource.getClass().getSimpleName());
                }
            }

        }

        private static String getQueryParametersFromMethod(final Method m) {
            final Set<String> params = new TreeSet<>();
            for (final Parameter par : m.getParameters()) {
                if (par.isAnnotationPresent(QueryParam.class)) {
                    params.add(par.getAnnotation(QueryParam.class).value());
                }
            }
            return params.isEmpty() ? "" : "?" + String.join("&", params);
        }

        /**
         * @param key The key of the resource (REST Method + URI)
         * @return the RAML resource that this key would refer to of null if no such method exists
         *
         */
        public RamlResource getResource(final String key) {
            if (this.fixedPaths.containsKey(key)) {
                return this.fixedPaths.get(key);
            }

            for (final Map.Entry<Pattern, RamlResource> entry : this.wildcardPaths.entrySet()) {
                if (entry.getKey().matcher(key).matches()) {
                    return entry.getValue();
                }
            }

            RamlResourceRegistry.log.warn("No resource found for \"{}\"", key);
            return null;
        }

        /**
         * @param m
         * @return
         */
        private static String restMethodOf(final Method m) {
            for (final Class<? extends Annotation> type : Arrays
                    .asList(GET.class, HEAD.class, POST.class, PUT.class, DELETE.class, OPTIONS.class, PATCH.class)) {
                if (m.isAnnotationPresent(type)) {
                    return type.getSimpleName();
                }
            }
            return null;
        }

        /**
         * Get the Pattern for a path that we are adding. This should replace any occurences of "{X}" by a wilcard
         * fragment.
         *
         * @param path The path to create a valid pattern out of
         * @return The pattern that will match the path
         */
        static Pattern getPattern(final String path) {
            final String WILD_FRAGMENT = "[^/\\{\\}]+";

            // This is the pattern to find the param groups in the path
            final Pattern param = Pattern.compile("\\{" + WILD_FRAGMENT + "\\}");
            final Matcher m = param.matcher(path);

            // Build the pattern using a stringbuilder
            int pos = 0;
            final StringBuilder pathPattern = new StringBuilder();
            while (m.find(pos)) {
                pathPattern.append(path.substring(pos, m.start()));
                pathPattern.append("(" + WILD_FRAGMENT + ")");
                pos = m.end();
            }

            // The rest is the tail, compile and return
            pathPattern.append(path.substring(pos));
            return Pattern.compile(pathPattern.toString());
        }

    }

}
