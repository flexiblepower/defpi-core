package org.flexiblepower.echo;

import java.io.Serializable;
import java.util.Properties;

import org.flexiblepower.service.MessageHandlerFactory;
import org.flexiblepower.service.Service;

public class EchoService implements Service, EchoMessageHandlerFactory {

	@Override
	public void resumeFrom(Serializable state) {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(Properties props) {
		// TODO Auto-generated method stub

	}

	@Override
	public void modify(Properties props) {
		// TODO Auto-generated method stub

	}

	@Override
	public Serializable suspend() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void terminate() {
		// TODO Auto-generated method stub

	}

	@Override
	public MessageHandlerFactory getMessageHandlerFactory() {
		return this;
	}

	@Override
	public EfiMessageHandler onEfiConnection() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WidgetMessageHandler onWidgetConnection() {
		// TODO Auto-generated method stub
		return null;
	}

}
