// AUTO-GENERATED FILE - This file will be overwritten when the code generation is executed!

package {{package}};

import java.lang.reflect.InvocationTargetException;
import org.flexiblepower.service.Service;
import org.flexiblepower.service.InterfaceHandler;

public class ServiceImpl extends Service {

	ServiceImpl() {
		super();
		try {
{{handlers}}
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