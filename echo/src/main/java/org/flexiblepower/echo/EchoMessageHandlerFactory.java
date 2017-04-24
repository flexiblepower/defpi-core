// GENERATED

package org.flexiblepower.echo;

import org.flexiblepower.service.MessageHandlerFactory;

public interface EchoMessageHandlerFactory extends MessageHandlerFactory {

	public EfiMessageHandler onEfiConnection();
	
	public WidgetMessageHandler onWidgetConnection();

}
