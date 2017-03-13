package org.flexiblepower.dsefpi.example.handlers;

import org.flexiblepower.service.ConnectionFactory;
import org.flexiblepower.service.PublishHandler;
import org.flexiblepower.service.ServiceSession;
import org.flexiblepower.service.SubscribeHandler;
import org.flexiblepower.service.Factory;
import org.flexiblepower.service.Service;

@Factory(interfaceName="Widget")

public class WidgetFactory extends ConnectionFactory {

	public WidgetFactory(Service service){
		super(service);
		// TODO Auto-generated constructor stub
	}

	@Override
	public SubscribeHandler<?> newSubscribeHandler(ServiceSession s) {
		// TODO Auto-generated method stub
		return new WidgetSubscribeHandler(s);
	}

	@Override
	public PublishHandler<?> newPublishHandler(ServiceSession s) {
		// TODO Auto-generated method stub
		return new WidgetPublishHandler(s);
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