package org.flexiblepower.echo;

import org.flexiblepower.service.ConnectionHandler;
import org.flexiblepower.service.ConnectionHandlerFactory;
import org.flexiblepower.service.Service;

public class EchoConnectionHandlerFactory implements ConnectionHandlerFactory {

	private final EchoService service;
	
	public EchoConnectionHandlerFactory(EchoService service) {
		this.service = service;
	}

	@Override
	public ConnectionHandler build() {
		return new EchoInterfaceHandler(service);
	}

}
