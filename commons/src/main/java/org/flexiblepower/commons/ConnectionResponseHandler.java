package org.flexiblepower.commons;

import org.flexiblepower.proto.ConnectionProto.ConnectionHandshake;
import org.flexiblepower.proto.ConnectionProto.ConnectionState;

public interface ConnectionResponseHandler {

    public void handleConnectionResponse(ConnectionHandshake message);

    public void timeOutOccurred();

    public ConnectionState expectedState();
}
