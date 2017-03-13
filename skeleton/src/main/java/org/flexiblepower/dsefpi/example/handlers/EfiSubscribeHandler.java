package org.flexiblepower.dsefpi.example.handlers;

import org.flexiblepower.service.ServiceSession;
import org.flexiblepower.service.SubscribeHandler;
import org.flexiblepower.service.XSDMessageSerializer;
import org.flexiblepower.dsefpi.example.xml.*;


public class EfiSubscribeHandler extends SubscribeHandler<Object> {
	
	public EfiSubscribeHandler(ServiceSession s){
		super(s);
		serializer = new XSDMessageSerializer(CurtailmentAllocation.class, AllocationRevoke.class);
	}

	public void receiveMessage(Object message) {
		// TODO Auto-generated method stub
	}
}