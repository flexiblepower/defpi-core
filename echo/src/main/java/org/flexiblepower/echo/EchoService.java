package org.flexiblepower.echo;

import java.io.Serializable;
import java.util.Properties;

import org.flexiblepower.echo.handlers.Echo101ConnectionHandler;
import org.flexiblepower.service.ConnectionManager;
import org.flexiblepower.service.Service;

public class EchoService implements Service {
	
	public EchoService() {
		ConnectionManager.registerHandlers(Echo101ConnectionHandler.class, new EchoConnectionHandlerFactory(this));
	}
	
	@Override
	public void resumeFrom(Serializable state) {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(Properties props) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void modify(Properties props) {
		// TODO Auto-generated method stub
	}

	@Override
	public Serializable suspend() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void terminate() {
		// TODO Auto-generated method stub
	}

	
}
