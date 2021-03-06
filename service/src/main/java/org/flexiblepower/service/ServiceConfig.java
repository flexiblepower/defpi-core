/*-
 * #%L
 * dEF-Pi service managing library
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
package org.flexiblepower.service;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ServiceConfig
 *
 * @version 0.1
 * @since 24 aug. 2017
 */
final class ServiceConfig {

    /**
     * Generate a configuration object that implements a specific class by using a map of key/value pairs. This will
     * create a Proxy object mimicking the provided class.
     *
     * @param clazz the configuration class to mimic.
     * @param rawValues key/value pairs containing the parameters.
     * @return An object of the type clazz, that will return the values in the map when corresponding getKey functions
     *         are called.
     */
    @SuppressWarnings("unchecked")
    static <T> T generateConfig(final Class<T> clazz, final Map<String, String> rawValues) {
        if (clazz.equals(Void.class)) {
            return null;
        }
        return (T) Proxy
                .newProxyInstance(clazz.getClassLoader(), new Class[] {clazz}, new GeneratedConfigHandler(rawValues));
    }

    /**
     * GeneratedConfigHandler
     *
     * @version 0.1
     * @since 24 aug. 2017
     */
    private final static class GeneratedConfigHandler implements InvocationHandler {

        private static final Logger log = LoggerFactory.getLogger(GeneratedConfigHandler.class);

        private final Map<String, String> values;

        private final Function<String, Boolean> convertBool = val -> val.isEmpty() ? false : Boolean.parseBoolean(val);
        private final Function<String, Byte> convertByte = val -> val.isEmpty() ? (byte) 0 : Byte.decode(val);
        private final Function<String, Character> convertChar = val -> val.isEmpty() ? (char) 0 : val.charAt(0);
        private final Function<String, Short> convertShort = val -> val.isEmpty() ? (short) 0 : Short.decode(val);
        private final Function<String, Integer> convertInteger = val -> val.isEmpty() ? 0 : Integer.decode(val);
        private final Function<String, Long> convertLong = val -> val.isEmpty() ? 0L : Long.decode(val);
        private final Function<String, Float> convertFloat = val -> val.isEmpty() ? (float) 0.0 : Float.parseFloat(val);
        private final Function<String, Double> convertDouble = val -> val.isEmpty() ? 0.0 : Double.parseDouble(val);

        /**
         * Creates a generated config InvocationHandler for the given key/value map.
         *
         * @param rawValues a key/value map containing the configuration parameters
         */
        GeneratedConfigHandler(final Map<String, String> rawValues) {
            this.values = new HashMap<>();
            rawValues.forEach((k, v) -> this.values.put(k.toLowerCase(), v));
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method,
         * java.lang.Object[])
         */
        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) {
            final String methodName = method.getName();

            if (!methodName.startsWith("get")) {
                throw new IllegalArgumentException("Can only invoke 'getters'");
            }

            // We assume camelCaps
            final String key = methodName.substring(3).toLowerCase();

            String rawValue = "";
            if (!this.values.containsKey(key)) {
                if (method.isAnnotationPresent(DefaultValue.class)) {
                    rawValue = method.getAnnotation(DefaultValue.class).value();
                } else {
                    GeneratedConfigHandler.log.warn("No parameter found with name \"{}\", returning type default",
                            method.getName().substring(3));
                }
            } else {
                rawValue = this.values.get(key);
            }

            if (method.getReturnType().equals(String.class)) {
                return rawValue;
            } else if (method.getReturnType().equals(boolean.class)) {
                return this.convertBool.apply(rawValue);
            } else if (method.getReturnType().equals(byte.class)) {
                return this.convertByte.apply(rawValue);
            } else if (method.getReturnType().equals(char.class)) {
                return this.convertChar.apply(rawValue);
            } else if (method.getReturnType().equals(short.class)) {
                return this.convertShort.apply(rawValue);
            } else if (method.getReturnType().equals(int.class)) {
                return this.convertInteger.apply(rawValue);
            } else if (method.getReturnType().equals(long.class)) {
                return this.convertLong.apply(rawValue);
            } else if (method.getReturnType().equals(float.class)) {
                return this.convertFloat.apply(rawValue);
            } else if (method.getReturnType().equals(double.class)) {
                return this.convertDouble.apply(rawValue);
            } else if (method.getReturnType().isArray()) {
                final String value = rawValue.replaceAll("([^a-zA-Z0-9,\\.])", "");
                final String[] cleanValues = value.isEmpty() ? new String[0] : value.split(",");

                if (method.getReturnType().equals(String[].class)) {
                    return cleanValues;
                } else if (method.getReturnType().equals(boolean[].class)) {
                    return this.parseBooleanArray(cleanValues);
                } else if (method.getReturnType().equals(byte[].class)) {
                    return this.parseByteArray(cleanValues);
                } else if (method.getReturnType().equals(char[].class)) {
                    return this.parseCharArray(cleanValues);
                } else if (method.getReturnType().equals(short[].class)) {
                    return this.parseShortArray(cleanValues);
                } else if (method.getReturnType().equals(int[].class)) {
                    return this.parseIntArray(cleanValues);
                } else if (method.getReturnType().equals(long[].class)) {
                    return this.parseLongArray(cleanValues);
                } else if (method.getReturnType().equals(float[].class)) {
                    return this.parseFloatArray(cleanValues);
                } else if (method.getReturnType().equals(double[].class)) {
                    return this.parseDoubleArray(cleanValues);
                } else {
                    throw new IllegalArgumentException(
                            "Unable to return array of type '" + method.getReturnType() + "'");
                }
            } else {
                throw new IllegalArgumentException(
                        "Unable to return parameter of type '" + method.getReturnType() + "'");
            }
        }

        private boolean[] parseBooleanArray(final String[] cleanValues) {
            final boolean[] ret = new boolean[cleanValues.length];
            IntStream.range(0, ret.length).forEach(i -> ret[i] = this.convertBool.apply(cleanValues[i]));
            return ret;
        }

        private byte[] parseByteArray(final String[] cleanValues) {
            final byte[] ret = new byte[cleanValues.length];
            IntStream.range(0, ret.length).forEach(i -> ret[i] = this.convertByte.apply(cleanValues[i]));
            return ret;
        }

        private char[] parseCharArray(final String[] cleanValues) {
            final char[] ret = new char[cleanValues.length];
            IntStream.range(0, ret.length).forEach(i -> ret[i] = this.convertChar.apply(cleanValues[i]));
            return ret;
        }

        private short[] parseShortArray(final String[] cleanValues) {
            final short[] ret = new short[cleanValues.length];
            IntStream.range(0, ret.length).forEach(i -> ret[i] = this.convertShort.apply(cleanValues[i]));
            return ret;
        }

        private int[] parseIntArray(final String[] cleanValues) {
            return Arrays.stream(cleanValues).map(this.convertInteger).mapToInt(Integer::intValue).toArray();
        }

        private long[] parseLongArray(final String[] cleanValues) {
            return Arrays.stream(cleanValues).map(this.convertLong).mapToLong(Long::longValue).toArray();
        }

        private float[] parseFloatArray(final String[] cleanValues) {
            final float[] ret = new float[cleanValues.length];
            IntStream.range(0, ret.length).forEach(i -> ret[i] = this.convertFloat.apply(cleanValues[i]));
            return ret;
        }

        private double[] parseDoubleArray(final String[] cleanValues) {
            return Arrays.stream(cleanValues).map(this.convertDouble).mapToDouble(Double::doubleValue).toArray();
        }

    }

}
