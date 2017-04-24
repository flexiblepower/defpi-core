package org.flexiblepower.echo.handlers;

import org.flexiblepower.service.ServiceSession;
import org.flexiblepower.service.SubscribeHandler;
import org.flexiblepower.service.ProtobufMessageSerializer;
import org.flexiblepower.echo.protobuf.MessageProto.Message;
import com.google.protobuf.GeneratedMessage;


public class EchoSubscribeHandler extends SubscribeHandler<GeneratedMessage> {
	
	public EchoSubscribeHandler(ServiceSession s){
		super(s);
		serializer = new ProtobufMessageSerializer(Message.class);
	}

	public void receiveMessage(GeneratedMessage message) {
		// TODO Auto-generated method stub
		Message recv = (Message) message;
		Message response = Message.newBuilder().setCounter(recv.getCounter()).setStr("Response!").build();
		//this.getServiceSession().getPublishHandler().publish(body)
	}
}