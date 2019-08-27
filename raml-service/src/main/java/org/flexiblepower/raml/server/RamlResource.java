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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RamlResource
 *
 * @version 0.1
 * @since Aug 20, 2019
 */
class RamlResource {

    private final Object resource;
    private final Method method;
    private final String uri;

    private Pattern uriPattern;

    /**
     * Create a RAML resource for a specific method of an object, which is available at a specific URI
     *
     * @param o the RAML resource
     * @param m the method of the resource
     * @param uri the URI where the method is available
     */
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

    /**
     * @return The original location how this resource is registered, this means the path parameters are represented as
     *         their keys enclosed in brackets "{}"
     */
    public String getUri() {
        return this.uri;
    }

    /**
     * @param args The arguments used for the method call
     * @return the result of dispatching the method represented by this object on obj with parameters args
     * @throws InvocationTargetException if the underlying method throws an exception
     * @throws IllegalArgumentException if the method is an instance method and the specified object argument is not an
     *             instance of the class or interface declaring the underlying method (or of a subclass or implementor
     *             thereof); if the number of actual and formal parameters differ; if an unwrapping conversion for
     *             primitive arguments fails; or if, after possible unwrapping, a parameter value cannot be converted to
     *             the corresponding formal parameter type by a method invocation conversion.
     * @throws IllegalAccessException if this Method object is enforcing Java language access control and the underlying
     *             method is inaccessible.
     * @see Method#invoke(Object, Object...)
     */
    public Object invoke(final Object... args) throws IllegalAccessException,
            IllegalArgumentException,
            InvocationTargetException {
        return this.method.invoke(this.resource, args);
    }

    /**
     * @return An array of parameters of the java method of this resource
     * @see Method#getParameters()
     */
    public Parameter[] getParameters() {
        return this.method.getParameters();
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
