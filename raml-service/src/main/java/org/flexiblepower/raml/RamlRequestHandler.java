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

import java.util.HashMap;
import java.util.Map;

import org.flexiblepower.proto.RamlProto.RamlRequest;
import org.flexiblepower.service.ConnectionHandler;
import org.flexiblepower.service.exceptions.ServiceInvocationException;
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
    private static Map<ConnectionHandler, RamlResourceRegistry> RESOURCES = new HashMap<>();

    /**
     * @param handler
     * @param message
     */
    public static void handle(final ConnectionHandler handler, final RamlRequest message) {
        if (!RamlRequestHandler.RESOURCES.containsKey(handler)) {
            RamlRequestHandler.RESOURCES.put(handler, new RamlResourceRegistry(handler));
        }

        try {
            RamlRequestHandler.RESOURCES.get(handler).getResourceForMessage(message);
        } catch (final ServiceInvocationException e) {
            RamlRequestHandler.log.error("Exception while invoking RAML resource: {}", e.getMessage());
            RamlRequestHandler.log.trace(e.getMessage(), e);
        }

        // final String key = message.getMethod().toString() + " " + message.getUri();
        //
        // if (RamlRequestHandler.RESOURCES.containsKey(handler)
        // && RamlRequestHandler.RESOURCES.get(handler).containsKey(key)) {
        // final ResourceEntry re = RamlRequestHandler.RESOURCES.get(handler).get(key);
        // try {
        // re.m.invoke(re.o);
        // } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        // RamlRequestHandler.log.error("Unable to invoke method: {}", e.getMessage());
        // RamlRequestHandler.log.trace(e.getMessage(), e);
        // }
        // } else {
        // RamlRequestHandler.log.warn("Unknown operation {} for handler {}", key, handler.getClass());
        // }
    }

}
