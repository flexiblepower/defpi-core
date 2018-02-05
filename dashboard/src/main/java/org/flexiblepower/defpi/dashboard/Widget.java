package org.flexiblepower.defpi.dashboard;

public interface Widget extends HttpHandler {

    public static enum Type {
        FULL,
        SMALL
    }

    /**
     * Name which can be used in the URL.
     *
     * @return
     */
    String getFullWidgetId();

    String getTitle();

    Widget.Type getType();

    boolean isActive();

}
