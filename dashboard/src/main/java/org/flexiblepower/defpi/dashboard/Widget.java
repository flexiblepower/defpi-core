package org.flexiblepower.defpi.dashboard;

public interface Widget extends HttpHandler {

	public static enum Type {
		FULL, SMALL
	}

	String getName();

	String getTitle();

	Widget.Type getType();

}
