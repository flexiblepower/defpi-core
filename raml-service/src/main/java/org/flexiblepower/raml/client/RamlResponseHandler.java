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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.flexiblepower.proto.RamlProto.RamlResponse;
import org.flexiblepower.service.ConnectionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RamlResponseHandler
 *
 * @version 0.1
 * @since Aug 26, 2019
 */
public class RamlResponseHandler {

    private static final Logger log = LoggerFactory.getLogger(RamlResponseHandler.class);
    public static final long TIMEOUT_MILLISECONDS = 30000;

    private static Map<Integer, Object> requestLocks = new HashMap<>();
    private static Map<Integer, RamlResponse> responses = new HashMap<>();

    /**
     * Handle a message by invoking the corresponding resource
     *
     * @param handler The handler that provided this reponse, and whose request is answered
     * @param message The message to handle
     */
    public static void handle(final ConnectionHandler handler, final RamlResponse message) {
        // TODO: Right now the handler is not actually used, this should be implemented
        final int id = message.getId();
        RamlResponseHandler.responses.put(id, message);

        if (RamlResponseHandler.requestLocks.containsKey(id)) {
            synchronized (RamlResponseHandler.requestLocks.get(id)) {
                RamlResponseHandler.requestLocks.get(id).notify();
            }
        }
    }

    /**
     * @param messageId The id of the message to get the response of
     * @return The RAML response to the message with the provided id
     * @throws InterruptedException When the thread is interrupted before the message arrives
     * @throws TimeoutException When more than {@value #TIMEOUT_MILLISECONDS} milliseconds passed since waiting for the
     *             response.
     */
    synchronized static RamlResponse getResponse(final int messageId) throws InterruptedException, TimeoutException {
        if (RamlResponseHandler.responses.containsKey(messageId)) {
            return RamlResponseHandler.responses.remove(messageId);
        }

        if (!RamlResponseHandler.requestLocks.containsKey(messageId)) {
            RamlResponseHandler.requestLocks.put(messageId, new Object());
        }

        synchronized (RamlResponseHandler.requestLocks.get(messageId)) {
            RamlResponseHandler.requestLocks.get(messageId).wait(RamlResponseHandler.TIMEOUT_MILLISECONDS);
        }

        if (RamlResponseHandler.responses.containsKey(messageId)) {
            return RamlResponseHandler.responses.remove(messageId);
        } else {
            RamlResponseHandler.log.error("Unable to get response from RAML");
            throw new TimeoutException("RAML request timed out.");
        }
    }

}
