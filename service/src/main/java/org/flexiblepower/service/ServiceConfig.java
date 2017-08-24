/**
 * File ServiceConfig.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

/**
 * ServiceConfig
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 24 aug. 2017
 */
public class ServiceConfig {

    @SuppressWarnings("unchecked")
    static <T> T generateConfig(final Class<T> clazz, final Map<String, String> rawValues) {
        return (T) Proxy
                .newProxyInstance(clazz.getClassLoader(), new Class[] {clazz}, new GeneratedConfigHandler(rawValues));
    }

    /**
     * GeneratedConfigHandler
     *
     * @author leeuwencjv
     * @version 0.1
     * @since 24 aug. 2017
     */
    private final static class GeneratedConfigHandler implements InvocationHandler {

        private final Map<String, String> values;

        /**
         * @param rawValues
         */
        public GeneratedConfigHandler(final Map<String, String> rawValues) {
            this.values = rawValues;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method,
         * java.lang.Object[])
         */
        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            final String methodName = method.getName();

            if (!methodName.startsWith("get")) {
                throw new IllegalArgumentException("Can only invoke 'getters'");
            }

            // We assume camelCaps
            final String key = methodName.substring(3, 4).toLowerCase() + methodName.substring(4, methodName.length());

            if (!this.values.containsKey(key)) {
                throw new IllegalArgumentException("Could not find method with name '" + methodName + "'");
            }

            final String rawValue = this.values.get(key);
            if (method.getReturnType().equals(String.class)) {
                return rawValue;
            } else if (method.getReturnType().equals(int.class)) {
                return Integer.parseInt(rawValue);
            } else if (method.getReturnType().equals(long.class)) {
                return Integer.parseInt(rawValue);
            } else if (method.getReturnType().equals(float.class)) {
                return Integer.parseInt(rawValue);
            } else if (method.getReturnType().equals(double.class)) {
                return Integer.parseInt(rawValue);
            } else {
                throw new IllegalArgumentException(
                        "Unable to return parameter of type '" + method.getReturnType() + "'");
            }
        }

    }

}
