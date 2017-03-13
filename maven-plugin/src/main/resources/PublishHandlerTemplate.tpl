package {{package}}.handlers;

import org.flexiblepower.service.ServiceSession;
import org.flexiblepower.service.PublishHandler;
import org.flexiblepower.service.{{descriptorSource}}MessageSerializer;
{{imports}}

public class {{name}}PublishHandler extends PublishHandler<{{type}}> {
	
	public {{name}}PublishHandler(ServiceSession s) {
		super(s);
		serializer = new {{descriptorSource}}MessageSerializer({{publishClasses}});
		// TODO Auto-generated constructor stub
	}
}