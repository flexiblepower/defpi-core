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
@SuppressWarnings({"javadoc", "static-method"})
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
        RamlResponseHandler.handle(msg);
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
