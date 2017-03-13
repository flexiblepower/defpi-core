package org.flexiblepower.dsefpi.example.handlers;

import org.flexiblepower.service.ServiceSession;
import org.flexiblepower.service.PublishHandler;
import org.flexiblepower.service.XSDMessageSerializer;
import org.flexiblepower.dsefpi.example.xml.*;


public class EfiPublishHandler extends PublishHandler<Object> {
	
	public EfiPublishHandler(ServiceSession s) {
		super(s);
		serializer = new XSDMessageSerializer(InflexibleRegistration.class, InflexibleUpdate.class, AllocationStatusUpdate.class, ControlSpaceRevoke.class);
		// TODO Auto-generated constructor stub
	}
}