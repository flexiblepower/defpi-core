package {{package}}.factories;

import org.flexiblepower.service.ConnectionHandler;
import org.flexiblepower.service.ConnectionHandlerFactory;
import org.flexiblepower.service.Service;
import {{package}}.handlers.{{handler.class}};

import javax.annotation.Generated;

/**
 * {{factory.class}}
 * 
 * @author {{username}}
 * @version {{service.version}}
 * @since {{date}}
 */
 @Generated({{generator}})
public class {{factory.class}} implements ConnectionHandlerFactory {

	@Override
	public ConnectionHandler build() {
		return new {{handler.class}}();
	}

}

