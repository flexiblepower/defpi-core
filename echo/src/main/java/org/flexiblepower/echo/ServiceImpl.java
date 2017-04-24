// AUTO-GENERATED FILE - This file will be overwritten when the code generation is executed!

package org.flexiblepower.echo;

import java.lang.reflect.InvocationTargetException;
import org.flexiblepower.service.Service;
import org.flexiblepower.service.InterfaceHandler;

public class ServiceImpl extends Service {

	ServiceImpl() {
		super();
		try {
			handlers.put("Echo Interface",new InterfaceHandler(
					"eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252", 
					"eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252", 
					getFactory("org.flexiblepower.echo.handlers", "Echo Interface").getDeclaredConstructor(Service.class).newInstance(this)));

			this.start();
		} catch (InstantiationException e) {
			System.out.println("Could not instantiate one of the factories\n\n"+e);
		} catch (IllegalAccessException e) {
			System.out.println("Could not find one of the factories\n\n"+e);
		} catch (IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			System.out.println("Could not instantiate factory\n\n"+e);
		}
	}

	public static void main(String[] args) {
		new ServiceImpl();
	}
	
	
	public MessageHandlerFactory getMessageHandlerFactory() {
		return new EchoMessageHandlerFactory();
	}
}