package {{package}}.handlers;

import org.flexiblepower.service.ConnectionFactory;
import org.flexiblepower.service.PublishHandler;
import org.flexiblepower.service.ServiceSession;
import org.flexiblepower.service.SubscribeHandler;
import org.flexiblepower.service.Factory;
import org.flexiblepower.service.Service;

@Factory(interfaceName="{{interfaceName}}")

public class {{name}}Factory extends ConnectionFactory {

	public {{name}}Factory(Service service){
		super(service);
		// TODO Auto-generated constructor stub
	}

	@Override
	public SubscribeHandler<?> newSubscribeHandler(ServiceSession s) {
		// TODO Auto-generated method stub
		return {{subscribeHandler}};
	}

	@Override
	public PublishHandler<?> newPublishHandler(ServiceSession s) {
		// TODO Auto-generated method stub
		return {{publishHandler}};
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