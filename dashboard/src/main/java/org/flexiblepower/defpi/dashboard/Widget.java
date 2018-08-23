package org.flexiblepower.defpi.dashboard;

public interface Widget extends HttpHandler {

    public static enum Type {
        FULL_WIDGET,
        SMALL_WIDGET,
        PAGE
    }

    /**
     * Name which can be used in the URL. Not used for small widgets.
     *
     * @return the Id of the Widget
     */
    String getWidgetId();

    String getTitle();

    Widget.Type getType();

}
