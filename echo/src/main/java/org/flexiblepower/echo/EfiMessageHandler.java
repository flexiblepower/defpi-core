// GENERATED

package org.flexiblepower.echo;

import org.flexiblepower.service.MessageHandler;

public interface EfiMessageHandler extends MessageHandler {

	void handleRegistrationMessage(RegistrationMessage msg);
	
	void handleUpdateMessage(UpdateMessage msg);
	
}
