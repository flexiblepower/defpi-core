package org.flexiblepower.echo.handlers;

import org.flexiblepower.service.ConnectionFactory;
import org.flexiblepower.service.PublishHandler;
import org.flexiblepower.service.ServiceSession;
import org.flexiblepower.service.SubscribeHandler;
import org.flexiblepower.service.Factory;
import org.flexiblepower.service.AbstractService;

@Factory(interfaceName="Echo Interface")

public class EchoFactory extends ConnectionFactory {

	public EchoFactory(AbstractService service){
		super(service);
		// TODO Auto-generated constructor stub
	}

	@Override
	public SubscribeHandler<?> newSubscribeHandler(ServiceSession s) {
		// TODO Auto-generated method stub
		return new EchoSubscribeHandler(s);
	}

	@Override
	public PublishHandler<?> newPublishHandler(ServiceSession s) {
		// TODO Auto-generated method stub
		return new EchoPublishHandler(s);
	}

	@Override
	public void deleteSubscribeHandler(SubscribeHandler<?> c) {
		// TODO Auto-generated method stub
	}

	@Override
	public void deletePublishHandler(PublishHandler<?> c) {
		// TODO Auto-generated method stub
	}
}