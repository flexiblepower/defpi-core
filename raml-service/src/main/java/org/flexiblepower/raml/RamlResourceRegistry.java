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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
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

import org.flexiblepower.proto.RamlProto.RamlRequest;
import org.flexiblepower.service.ConnectionHandler;
import org.flexiblepower.service.exceptions.ServiceInvocationException;
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
     * @param message
     * @return
     * @throws ServiceInvocationException
     */
    public void getResourceForMessage(final RamlRequest message) throws ServiceInvocationException {
        final String uri = message.getUri();
        final int delim = uri.indexOf('/', 1);
        final String top = uri.substring(0, delim > 0 ? delim : uri.length());

        if (!this.resources.containsKey(top)) {
            throw new ServiceInvocationException("Unable to locate resource at path " + top);
        }

        final MethodRegistry mr = this.resources.get(top);
        try {
            mr.invoke(message.getMethod().name() + " " + uri.substring(top.length()));
        } catch (NoSuchMethodException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException e) {
            throw new ServiceInvocationException("Exception while invoking resource", e);
        }
    }

    /**
     * MethodRegistry
     *
     * @version 0.1
     * @since Aug 10, 2019
     */
    static class MethodRegistry {

        private final Object resource;

        private final Map<String, Method> fixedPaths = new HashMap<>();
        private final Map<Pattern, Method> wildcardPaths = new HashMap<>();

        /**
         * Create a MethodRegistry for a resource. That is find any methods that correspond to a REST endpoint and parse
         * them already to reduce processing later on.
         *
         * @param r The resource to build the registry of
         */
        MethodRegistry(final Object r) {
            this.resource = r;

            Class<?> clazz = r.getClass();
            if (!clazz.isAnnotationPresent(Path.class)) {
                for (final Class<?> itf : clazz.getInterfaces()) {
                    if (itf.isAnnotationPresent(Path.class)) {
                        clazz = itf;
                        break;
                    }
                }
            }
            if (!clazz.isAnnotationPresent(Path.class)) {
                RamlResourceRegistry.log.error("Unable to determine the Path annotated superclass of {}", r.getClass());
            }

            for (final Method m : clazz.getMethods()) {
                // Here I assume there is only one restMethod per method.
                final String restMethod = MethodRegistry.restMethodOf(m);
                if (restMethod == null) {
                    continue;
                }

                if (!m.isAnnotationPresent(Path.class)) {
                    // This is a simple method, no extra path
                    this.fixedPaths.put(restMethod, m);
                    RamlResourceRegistry.log.debug("Added {} for resource path {}",
                            restMethod,
                            this.resource.getClass().getSimpleName());
                    continue;
                }

                final String typePath = m.getAnnotation(Path.class).value();
                if (!typePath.contains("{")) {
                    // This is a method with some additional path
                    this.fixedPaths.put(restMethod + typePath, m);
                    RamlResourceRegistry.log.debug("Added {} for resource path {}",
                            restMethod + typePath,
                            this.resource.getClass().getSimpleName());
                } else {
                    // This method has a path with a parameter in it
                    this.wildcardPaths.put(MethodRegistry.getPattern(restMethod + typePath), m);
                    RamlResourceRegistry.log.debug("Added \"{}\" for resource path {}",
                            restMethod + typePath,
                            this.resource.getClass().getSimpleName());
                }
            }

        }

        /**
         * @param substring
         * @throws NoSuchMethodException
         * @throws InvocationTargetException
         * @throws IllegalAccessException
         * @throws IllegalArgumentException
         */
        public void invoke(final String key) throws NoSuchMethodException,
                IllegalAccessException,
                IllegalArgumentException,
                InvocationTargetException {
            if (this.fixedPaths.containsKey(key)) {
                final Method m = this.fixedPaths.get(key);
                m.invoke(this.resource);
            }

            for (final Map.Entry<Pattern, Method> entry : this.wildcardPaths.entrySet()) {
                if (entry.getKey().matcher(key).matches()) {
                    final Method m = entry.getValue();
                    m.invoke(this.resource);
                }
            }

            throw new NoSuchMethodException("Unable to find method " + key + " for resource " + this.resource);
        }

        /**
         * @param m
         * @return
         */
        private static String restMethodOf(final Method m) {
            if (m.isAnnotationPresent(GET.class)) {
                return "GET ";
            }
            if (m.isAnnotationPresent(HEAD.class)) {
                return "HEAD ";
            }
            if (m.isAnnotationPresent(POST.class)) {
                return "POST ";
            }
            if (m.isAnnotationPresent(PUT.class)) {
                return "PUT ";
            }
            if (m.isAnnotationPresent(DELETE.class)) {
                return "DELETE ";
            }
            if (m.isAnnotationPresent(OPTIONS.class)) {
                return "OPTIONS ";
            }
            if (m.isAnnotationPresent(PATCH.class)) {
                return "PATCH ";
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
