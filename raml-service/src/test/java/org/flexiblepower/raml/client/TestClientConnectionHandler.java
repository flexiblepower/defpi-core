/*-
 * #%L
 * dEF-Pi RAML services
 * %%
 * Copyright (C) 2017 - 2019 Flexible Power Alliance Network
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
/**
 * File TestClientConnectionHandler.java
 *
 * Copyright 2019 FAN
 */
package org.flexiblepower.raml.client;

import org.flexiblepower.proto.RamlProto.RamlRequest;
import org.flexiblepower.proto.RamlProto.RamlResponse;
import org.flexiblepower.serializers.ProtobufMessageSerializer;
import org.flexiblepower.service.Connection;
import org.flexiblepower.service.ConnectionHandler;
import org.flexiblepower.service.InterfaceInfo;

/**
 * TestClientConnectionHandler
 *
 * @version 0.1
 * @since Sep 6, 2019
 */
@SuppressWarnings("javadoc")
@InterfaceInfo(name = "Test client connection handler",
               version = "client",
               receivesHash = "54321",
               sendsHash = "12345",
               serializer = ProtobufMessageSerializer.class,
               sendTypes = {RamlRequest.class},
               receiveTypes = {RamlResponse.class})
public class TestClientConnectionHandler implements ConnectionHandler {

    public TestClientConnectionHandler(final Connection c) {
        // TODO Auto-generated constructor stub
    }

    // This would normally be implemented as a default implementation in the generated code
    public void handleRamlResponse(final RamlResponse msg) {
        RamlResponseHandler.handle(this, msg);
    }

    @Override
    public void onSuspend() {
        // TODO Auto-generated method stub
    }

    @Override
    public void resumeAfterSuspend() {
        // TODO Auto-generated method stub
    }

    @Override
    public void onInterrupt() {
        // TODO Auto-generated method stub
    }

    @Override
    public void resumeAfterInterrupt() {
        // TODO Auto-generated method stub
    }

    @Override
    public void terminated() {
        // TODO Auto-generated method stub
    }

}
