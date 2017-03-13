package {{package}}.handlers;

import org.flexiblepower.service.ServiceSession;
import org.flexiblepower.service.SubscribeHandler;
import org.flexiblepower.service.{{descriptorSource}}MessageSerializer;
{{imports}}

public class {{name}}SubscribeHandler extends SubscribeHandler<{{type}}> {
	
	public {{name}}SubscribeHandler(ServiceSession s){
		super(s);
		serializer = new {{descriptorSource}}MessageSerializer({{subscribeClasses}});
	}

	public void receiveMessage({{type}} message) {
		// TODO Auto-generated method stub
	}
}