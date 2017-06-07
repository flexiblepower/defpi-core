package org.flexiblepower.echo;

import org.flexiblepower.echo.handlers.Echo101ConnectionHandler;
import org.flexiblepower.echo.protobuf.EchoInterfaceProto.Msg;
import org.flexiblepower.service.Connection;
import org.flexiblepower.service.Service;

import com.google.protobuf.GeneratedMessage;

public class EchoInterfaceHandler implements Echo101ConnectionHandler {
	
	private final EchoService service;
	private Connection connection;
	
	public EchoInterfaceHandler(EchoService service) {
		this.service = service;
	}

	@Override
	public GeneratedMessage handleMsgMessage(Msg message) {
		return Msg.newBuilder().setCounter(message.getCounter()).setStr("Response!").build();
	}

	@Override
	public void onConnected(Connection connection) {
		this.connection = connection;
		
		this.service.startSendingThread(connection);
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