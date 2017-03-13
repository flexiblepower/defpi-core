package org.flexiblepower.dsefpi.example.handlers;

import org.flexiblepower.service.ServiceSession;
import org.flexiblepower.service.PublishHandler;
import org.flexiblepower.service.ProtobufMessageSerializer;
import org.flexiblepower.dsefpi.example.protobuf.HTTPProto.HTTPResponse;
import com.google.protobuf.GeneratedMessage;


public class WidgetPublishHandler extends PublishHandler<GeneratedMessage> {
	
	public WidgetPublishHandler(ServiceSession s) {
		super(s);
		serializer = new ProtobufMessageSerializer(HTTPResponse.class);
		// TODO Auto-generated constructor stub
	}
}