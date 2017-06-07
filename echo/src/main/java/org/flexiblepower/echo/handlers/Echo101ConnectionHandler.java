// GENERATED

package org.flexiblepower.echo.handlers;

import org.flexiblepower.echo.protobuf.EchoInterfaceProto.Msg;
import org.flexiblepower.service.ConnectionHandler;
import org.flexiblepower.service.InterfaceInfo;
import org.flexiblepower.service.serializers.ProtobufMessageSerializer;

import com.google.protobuf.GeneratedMessage;

@InterfaceInfo(name = "Echo", version = "1.0.1", 
		receivesHash = "eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252", 
		sendsHash = "eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252", 
		serializer = ProtobufMessageSerializer.class, 
		receiveTypes = { Msg.class }, sendTypes = { Msg.class })
public interface Echo101ConnectionHandler extends ConnectionHandler {

	public GeneratedMessage handleMsgMessage(Msg message) throws Exception;

}