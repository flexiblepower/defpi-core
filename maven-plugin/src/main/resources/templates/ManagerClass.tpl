package {{service.package}}.{{itf.package}};

import javax.annotation.Generated;

import {{service.package}}.{{service.class}};
{{itf.manager.imports.implementation}}
import org.flexiblepower.service.Connection;

/**
 * Implementation of the connection manager for the {{itf.manager.interface}} interface.
 * Creates connection handlers for the supported versions of the interface.
 *
 * File generated by org.flexiblepower.create-service-maven-plugin. 
 * NOTE: This file is generated as a stub, and has to be implemented by the user. Re-running the codegen plugin will
 * 		 not change the contents of this file.
 * Template by TNO, 2017
 * 
 * @author {{username}}
 */
@Generated(value = "{{generator}}", date = "{{date}}")
public class {{itf.manager.class}} implements {{itf.manager.interface}} {
    /**
     * Instance of the {{service.class}} belonging to this connection manager.
     */
	private final {{service.class}} service;
	
	/**
	 * Auto-generated constructor building the factory for ConnectionHandlers of the provided service
	 *
	 * @param service The service that will be used as argument to instantiate the ConnectionHandlers
	 */
	public {{itf.manager.class}}({{service.class}} service) {
		this.service = service;
	}

{{itf.manager.implementations}}

}

