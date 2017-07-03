package {{package}}.{{itf.packagename}};

import javax.annotation.Generated;

import org.flexiblepower.service.ConnectionHandler;
import org.flexiblepower.service.ConnectionHandlerFactory;


/**
 * {{factory.class}}
 * 
 * @author {{username}}
 * @version {{service.version}}
 * @since {{date}}
 */
@Generated("{{generator}}")
public class {{factory.class}} implements ConnectionHandlerFactory {

	@Override
	public ConnectionHandler build() {
		return new {{handlerImpl.class}}();
	}

}

