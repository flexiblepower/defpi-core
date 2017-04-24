package org.flexiblepower.echo.handlers;

import org.flexiblepower.service.ServiceSession;
import org.flexiblepower.service.PublishHandler;
import org.flexiblepower.service.ProtobufMessageSerializer;
import org.flexiblepower.echo.protobuf.MessageProto.Message;
import com.google.protobuf.GeneratedMessage;


public class EchoPublishHandler extends PublishHandler<GeneratedMessage> {
	
	public EchoPublishHandler(ServiceSession s) {
		super(s);
		serializer = new ProtobufMessageSerializer(Message.class);
		// TODO Auto-generated constructor stub
	}
}