package org.flexiblepower.dsefpi.example.handlers;

import org.flexiblepower.service.ServiceSession;
import org.flexiblepower.service.SubscribeHandler;
import org.flexiblepower.service.ProtobufMessageSerializer;
import org.flexiblepower.dsefpi.example.protobuf.HTTPProto.HTTPRequest;
import com.google.protobuf.GeneratedMessage;


public class WidgetSubscribeHandler extends SubscribeHandler<GeneratedMessage> {
	
	public WidgetSubscribeHandler(ServiceSession s){
		super(s);
		serializer = new ProtobufMessageSerializer(HTTPRequest.class);
	}

	public void receiveMessage(GeneratedMessage message) {
		// TODO Auto-generated method stub
	}
}