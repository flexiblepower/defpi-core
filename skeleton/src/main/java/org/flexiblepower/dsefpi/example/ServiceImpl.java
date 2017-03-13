// AUTO-GENERATED FILE - This file will be overwritten when the code generation is executed!

package org.flexiblepower.dsefpi.example;

import java.lang.reflect.InvocationTargetException;
import org.flexiblepower.service.Service;
import org.flexiblepower.service.InterfaceHandler;

public class ServiceImpl extends Service {

	ServiceImpl() {
		super();
		try {
			handlers.put("Widget",new InterfaceHandler(
					"d711d463073b4333c021e363a590f5b3dbf0a140da4189d71ed3b81d7b87b5c1", 
					"6b068b7fac431c719d6600f31667da6e41bc96db61d6919e4c7251b0a15cd445", 
					getFactory("org.flexiblepower.dsefpi.example.handlers", "Widget").getDeclaredConstructor(Service.class).newInstance(this)));
			handlers.put("Efi",new InterfaceHandler(
					"375d1aa10c119dfe9f602ab99ec1442f9cf8032fcaa7269da998b52415b494f8", 
					"99707099c8667d42044cbaf4f92dc96c334aa442f6831a8cddedb6a655e8bfca", 
					getFactory("org.flexiblepower.dsefpi.example.handlers", "Efi").getDeclaredConstructor(Service.class).newInstance(this)));

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
}