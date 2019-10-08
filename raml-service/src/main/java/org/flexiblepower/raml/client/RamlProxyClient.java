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
package org.flexiblepower.raml.client;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.flexiblepower.proto.RamlProto.RamlRequest;
import org.flexiblepower.proto.RamlProto.RamlResponse;
import org.flexiblepower.service.Connection;
import org.flexiblepower.service.ConnectionHandler;
import org.flexiblepower.service.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * RamlProxyClient
 *
 * @version 0.1
 * @since Aug 22, 2019
 */
public class RamlProxyClient {

    /**
     * Generate a resource object that implements a specific class by wrapping a RamlMessage sending connection. This
     * will create a Proxy object mimicking the provided class.
     *
     * @param clazz the resource class to mimic.
     * @param handler The connection handler to send/receive RAML messages
     * @return An object of the type clazz, that will return the values in the map when corresponding getKey functions
     *         are called.
     */
    @SuppressWarnings("unchecked")
    public static <T> T generateClient(final Class<T> clazz, final ConnectionHandler handler) {
        return (T) Proxy
                .newProxyInstance(clazz.getClassLoader(), new Class[] {clazz}, new RamlProxyHandler(handler, clazz));
    }

    /**
     * RamlProxyHandler
     *
     * @version 0.1
     * @since Aug 22, 2019
     */
    private final static class RamlProxyHandler implements InvocationHandler {

        private static final Logger log = LoggerFactory.getLogger(RamlResponseHandler.class);

        private static int messageCounter = 0;
        private static ObjectMapper om = new ObjectMapper();
        private final ConnectionHandler handler;
        private final String typePath;

        /**
         * @param h The connection handler to send messages through
         * @param clazz The class to generate the proxy for
         */
        RamlProxyHandler(final ConnectionHandler h, final Class<?> clazz) {
            this.handler = h;

            // Other class annotated values are not yet used, e.g. @Consumes
            if (clazz.isAnnotationPresent(Path.class)) {
                this.typePath = clazz.getAnnotation(Path.class).value();
            } else {
                this.typePath = "";
            }
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            final Connection conn = ConnectionManager.getMyConnection(this.handler);
            if (conn == null) {
                throw new IllegalStateException("Unable to send a message when there is no connection");
            }

            final Map<String, Object> pathParams = new HashMap<>();
            final Set<String> queryParams = new HashSet<>();
            Object body = null;

            final Parameter[] params = method.getParameters();
            for (int i = 0; i < params.length; i++) {
                if (params[i].isAnnotationPresent(PathParam.class)) {
                    final PathParam param = params[i].getAnnotation(PathParam.class);
                    pathParams.put(param.value(), args[i]);
                } else if (params[i].isAnnotationPresent(QueryParam.class)) {
                    final QueryParam param = params[i].getAnnotation(QueryParam.class);
                    queryParams.add(param.value() + "=" + args[i].toString());
                } else if (body == null) {
                    body = args[i];
                } else {
                    throw new IllegalArgumentException("Unable to process parameters for argument " + i);
                }
            }

            // Get the method path
            String methodPath;
            if (method.isAnnotationPresent(Path.class)) {
                methodPath = method.getAnnotation(Path.class).value();
                if (!pathParams.isEmpty()) {
                    methodPath = RamlProxyHandler.fillPathParams(methodPath, pathParams);
                }

                if (!methodPath.startsWith("/")) {
                    methodPath = "/" + methodPath;
                }
            } else {
                methodPath = "";
            }

            // Append any query parameters
            if (!queryParams.isEmpty()) {
                methodPath = methodPath + "?" + String.join("&", queryParams);
            }

            final RamlRequest.Method ramlMethod = RamlProxyHandler.getRamlMethod(method);
            final RamlRequest.Builder builder = RamlRequest.newBuilder()
                    .setId(RamlProxyHandler.messageCounter++)
                    .setUri(this.typePath + methodPath)
                    .setMethod(ramlMethod);
            if (body != null) {
                builder.setBody(RamlProxyHandler.om.writeValueAsString(body));
            }

            final RamlRequest message = builder.build();
            conn.send(message);
            final RamlResponse response = RamlResponseHandler.getResponse(message.getId());

            final String responseString = response.getBody().toStringUtf8();

            if ((response.getStatus() > 300) && (response.getStatus() < 500)) {
                RamlProxyHandler.log.error("RAML client error {}: {}", response.getStatus(), responseString);
                throw new RuntimeException("Error invoking " + method.getName() + ": " + responseString);
            } else if (response.getStatus() >= 500) {
                final int pos = responseString.indexOf("::");
                @SuppressWarnings("unchecked")
                final Class<? extends Throwable> clazz = (Class<? extends Throwable>) Class
                        .forName(responseString.substring(0, pos));

                RamlProxyHandler.log.error("RAML server error {}: {}", response.getStatus(), responseString);
                final Throwable e = RamlProxyHandler.om.readValue(responseString.substring(pos + 2), clazz);
                throw e;
                // new RuntimeException("Error invoking " + method.getName() + ": " + responseString);
            }

            return RamlProxyHandler.om.readValue(response.getBody().toStringUtf8(), method.getReturnType());
        }

        private static final RamlRequest.Method getRamlMethod(final Method m) {
            for (final Class<? extends Annotation> type : Arrays
                    .asList(GET.class, HEAD.class, POST.class, PUT.class, DELETE.class, OPTIONS.class, PATCH.class)) {
                if (m.isAnnotationPresent(type)) {
                    return RamlRequest.Method.valueOf(type.getSimpleName());
                }
            }
            return RamlRequest.Method.GET;
        }

        private static final String fillPathParams(final String uri, final Map<String, Object> values) {
            String newUri = uri;
            for (final Map.Entry<String, Object> entry : values.entrySet()) {
                newUri = newUri.replace("{" + entry.getKey() + "}", entry.getValue().toString());
            }
            return newUri;
        }

    }

}
