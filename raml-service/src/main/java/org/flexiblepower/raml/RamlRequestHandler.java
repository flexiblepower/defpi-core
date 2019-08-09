/**
 * File RamlRequestHandler.java
 *
 * Copyright 2019 FAN
 */
package org.flexiblepower.raml;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Path;

import org.flexiblepower.proto.RamlProto.RamlRequest;
import org.flexiblepower.service.ConnectionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RamlRequestHandler
 *
 * @version 0.1
 * @since Aug 9, 2019
 */
public class RamlRequestHandler {

    private static final Logger log = LoggerFactory.getLogger(RamlRequestHandler.class);
    private static final Map<ConnectionHandler, Map<String, ResourceEntry>> RESOURCES = new HashMap<>();

    /**
     * @param handler
     * @param clazz
     */
    public static <T> void register(final ConnectionHandler handler, final Class<T> clazz) {
        if (!RamlRequestHandler.RESOURCES.containsKey(handler)) {
            RamlRequestHandler.RESOURCES.put(handler, new HashMap<>());
        }

        final T resource = RamlRequestHandler.getResourceForType(handler, clazz);

        if (clazz.isAnnotationPresent(Path.class)) {
            final Path typePath = clazz.getAnnotation(Path.class);
            for (final Method m : clazz.getMethods()) {
                String path = typePath.value();
                if (m.isAnnotationPresent(Path.class)) {
                    path += m.getAnnotation(Path.class).value();
                }

                if (m.isAnnotationPresent(javax.ws.rs.GET.class)) {
                    RamlRequestHandler.RESOURCES.get(handler).put("GET " + path, new ResourceEntry(resource, m));
                }
                if (m.isAnnotationPresent(javax.ws.rs.HEAD.class)) {
                    RamlRequestHandler.RESOURCES.get(handler).put("HEAD " + path, new ResourceEntry(resource, m));
                }
                if (m.isAnnotationPresent(javax.ws.rs.POST.class)) {
                    RamlRequestHandler.RESOURCES.get(handler).put("POST " + path, new ResourceEntry(resource, m));
                }
                if (m.isAnnotationPresent(javax.ws.rs.PUT.class)) {
                    RamlRequestHandler.RESOURCES.get(handler).put("PUT " + path, new ResourceEntry(resource, m));
                }
                if (m.isAnnotationPresent(javax.ws.rs.DELETE.class)) {
                    RamlRequestHandler.RESOURCES.get(handler).put("DELETE " + path, new ResourceEntry(resource, m));
                }
                if (m.isAnnotationPresent(javax.ws.rs.OPTIONS.class)) {
                    RamlRequestHandler.RESOURCES.get(handler).put("OPTIONS " + path, new ResourceEntry(resource, m));
                }
                if (m.isAnnotationPresent(javax.ws.rs.PATCH.class)) {
                    RamlRequestHandler.RESOURCES.get(handler).put("PATCH " + path, new ResourceEntry(resource, m));
                }
            }
            RamlRequestHandler.log.info("Added {} at path {}", resource.getClass().getSimpleName(), typePath.value());
        } else {
            throw new IllegalArgumentException("Could not find " + Path.class + " annotation on " + clazz);
        }
    }

    /**
     * @param handler
     * @param clazz
     * @return
     * @throws SecurityException
     * @throws NoSuchMethodException
     */
    @SuppressWarnings("unchecked")
    private static <T> T getResourceForType(final ConnectionHandler handler, final Class<T> clazz) {
        final String expectedMethodName = String.format("get%s", clazz.getSimpleName());
        try {
            final Method m = handler.getClass().getMethod(expectedMethodName);
            if (!m.getReturnType().equals(clazz)) {
                throw new IllegalArgumentException("Method " + expectedMethodName + " has unexpected return type");
            }
            return (T) m.invoke(handler);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("Unable to invoke method " + expectedMethodName, e);
        }
    }

    /**
     * @param handler
     * @param message
     */
    public static void handle(final ConnectionHandler handler, final RamlRequest message) {
        final String key = message.getMethod().toString() + " " + message.getUri();

        if (RamlRequestHandler.RESOURCES.containsKey(handler)
                && RamlRequestHandler.RESOURCES.get(handler).containsKey(key)) {
            final ResourceEntry re = RamlRequestHandler.RESOURCES.get(handler).get(key);
            try {
                re.m.invoke(re.o);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                RamlRequestHandler.log.error("Unable to invoke method: {}", e.getMessage());
                RamlRequestHandler.log.trace(e.getMessage(), e);
            }
        } else {
            RamlRequestHandler.log.warn("Unknown operation {} for handler {}", key, handler.getClass());
        }
    }

    private static class ResourceEntry {

        public ResourceEntry(final Object o, final Method m) {
            this.o = o;
            this.m = m;
        }

        public Object o;
        public Method m;

    }

}
