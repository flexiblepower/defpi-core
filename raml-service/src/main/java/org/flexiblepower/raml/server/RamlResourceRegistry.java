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
package org.flexiblepower.raml.server;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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

    @SuppressWarnings("javadoc")
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
     * @return The RAML request that this message should invoke
     */
    public RamlResourceRequest getResourceForMessage(final RamlRequest message) {
        String uri = message.getUri();
        final Map<String, String> queryParams = RamlResourceRequest.getQueryParametersFromUri(uri);
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
        final String key = message.getMethod().name() + " " + top + uri.substring(top.length()) + suffixFromQueryParams;

        final RamlResource ret = this.resources.get(top).getResource(key);
        if (ret != null) {
            return new RamlResourceRequest(ret, uri, queryParams);
        }
        return null;
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
                return;
            }

            final String resourcePath = clazz.getAnnotation(Path.class).value();

            for (final Method m : clazz.getMethods()) {
                // Here I assume there is only one restMethod per method.
                final String restMethod = MethodRegistry.restMethodOf(m);
                if (restMethod == null) {
                    continue;
                }
                final String methodPath = MethodRegistry.methodPathOf(m);
                final String queryParams = MethodRegistry.queryParametersOf(m);

                final RamlResource ramlResource = new RamlResource(resource, m, resourcePath + methodPath);

                if (methodPath.contains("{")) {
                    final Pattern pat = ramlResource.getUriPattern();
                    this.wildcardPaths.put(Pattern.compile(restMethod + pat + queryParams), ramlResource);
                } else {
                    this.fixedPaths.put(restMethod + resourcePath + methodPath + queryParams, ramlResource);
                }

                RamlResourceRegistry.log.debug("Added \"{}\" for resource path {}",
                        ramlResource.getUri(),
                        resource.getClass().getSimpleName());
            }

        }

        /**
         * @param key The key of the resource (REST Method + URI + query parameters)
         * @return the RAML resource that this key would refer to of null if no such method exists
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

        private static String restMethodOf(final Method m) {
            for (final Class<? extends Annotation> type : Arrays
                    .asList(GET.class, HEAD.class, POST.class, PUT.class, DELETE.class, OPTIONS.class, PATCH.class)) {
                if (m.isAnnotationPresent(type)) {
                    return type.getSimpleName() + " ";
                }
            }
            return null;
        }

        private static String queryParametersOf(final Method m) {
            final Set<String> params = new TreeSet<>();
            for (final Parameter par : m.getParameters()) {
                if (par.isAnnotationPresent(QueryParam.class)) {
                    params.add(par.getAnnotation(QueryParam.class).value());
                }
            }
            return params.isEmpty() ? "" : "?" + String.join("&", params);
        }

        private static String methodPathOf(final Method m) {
            if (m.isAnnotationPresent(Path.class)) {
                final String value = m.getAnnotation(Path.class).value();
                return value.startsWith("/") ? value : "/" + value;
            }
            return "";
        }

    }

}
