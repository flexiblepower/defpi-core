package org.flexiblepower.defpi.dashboard.widget;

/*-
 * #%L
 * dEF-Pi dashboard
 * %%
 * Copyright (C) 2017 - 2018 Flexible Power Alliance Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import javax.annotation.Generated;

import org.flexiblepower.defpi.dashboard.Dashboard;
import org.flexiblepower.defpi.dashboard.widget.http.Widget_httpConnectionHandlerImpl;
import org.flexiblepower.defpi.dashboard.widget.http.Widget_httpConnectionHandler;
import org.flexiblepower.service.Connection;

/**
 * WidgetConnectionManagerImpl
 *
 * File generated by org.flexiblepower.create-service-maven-plugin. 
 * NOTE: This file is generated as a stub, and has to be implemented by the user. Re-running the codegen plugin will
 * 		 not change the contents of this file.
 * Template by TNO, 2017
 * 
 * @author wilco
 */
@Generated(value = "org.flexiblepower.plugin.servicegen", date = "Oct 26, 2017 12:58:33 PM")
public class WidgetConnectionManagerImpl implements WidgetConnectionManager {

	private final Dashboard service;
	
	/**
	 * Auto-generated constructor building the factory for ConnectionHandlers of the provided service
	 *
	 * @param service The service that will be used as argument to instantiate the ConnectionHandlers
	 */
	public WidgetConnectionManagerImpl(Dashboard service) {
		this.service = service;
	}

	@Override
	public Widget_httpConnectionHandler buildHttp(Connection connection) {
		return new Widget_httpConnectionHandlerImpl(connection, this.service);
	}

}

