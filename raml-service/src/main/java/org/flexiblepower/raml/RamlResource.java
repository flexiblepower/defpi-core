/**
 * File RamlResource.java
 *
 * Copyright 2019 FAN
 */
package org.flexiblepower.raml;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.regex.Pattern;

import org.flexiblepower.proto.RamlProto.RamlRequest;

/**
 * RamlResource
 *
 * @version 0.1
 * @since Aug 20, 2019
 */
public class RamlResource {

    private final Object resource;
    private final Method method;
    private final Pattern pattern;
    private String uri;
    private Map<String, String> queryParameters;

    RamlResource(final Object o, final Method m) {
        this.resource = o;
        this.method = m;
        this.pattern = null;
    }

    RamlResource(final Object o, final Method m, final Pattern p) {
        this.resource = o;
        this.method = m;
        this.pattern = p;
    }

    public RamlResource withURI(final String uri) {
        this.uri = uri;
        return this;
    }

    public RamlResource withQueryParameters(final Map<String, String> params) {
        this.queryParameters = params;
        return this;
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
     * @see Method#invoke(Object, Object...)
     */
    public Object invoke(final RamlRequest message) throws IllegalAccessException,
            IllegalArgumentException,
            InvocationTargetException {
        return this.method.invoke(this.resource);
    }

}
