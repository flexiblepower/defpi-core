/**
 * File ApiException.java
 *
 * Copyright 2016 TNO
 */
package org.flexiblepower.exceptions;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * This exception is thrown by the API. When the API is accessed from a browser, it makes sure a user-friendly page is
 * displayed.
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 9 dec. 2016
 */
public class ApiException extends WebApplicationException {

    private static final long serialVersionUID = 2129777409204339916L;
    private static final Status DEFAULT_STATUS = Status.INTERNAL_SERVER_ERROR;
    private static final String DEFAULT_TITLE = "Error using API";
    private static final String DEFAULT_MESSAGE = "An error occured while processing your request";

    /**
     * Creates an Exception for the API containing a response which is a user friendly HTML showing the error message
     * and optionally the stacktrace of the causing exception.
     *
     * @param title The title of the page to display. If {@code null}, the default title will be used
     * @param message The message to display on the page. If {@code null}, a default message will be used
     * @param cause The cause of the exception, if {@code null} nothing will be displayed
     */
    protected ApiException(final Status status, final String title, final String message, final Throwable cause) {
        super(Response.status(status)
                .entity(ApiException.createErrorPage(title, message, cause))
                .type(MediaType.TEXT_HTML)
                .build());
    }

    public ApiException(final String message) {
        this(ApiException.DEFAULT_STATUS, ApiException.DEFAULT_TITLE, message, null);
    }

    public ApiException(final String message, final Throwable t) {
        this(ApiException.DEFAULT_STATUS, ApiException.DEFAULT_TITLE, message, t);
    }

    public ApiException(final Throwable t) {
        this(ApiException.DEFAULT_STATUS, ApiException.DEFAULT_TITLE, ApiException.DEFAULT_MESSAGE, t);
    }

    public ApiException(final Status status) {
        this(status, "");
    }

    public ApiException(final Status status, final Throwable t) {
        this(status, String.format("%s (%d)", status.getReasonPhrase(), status.getStatusCode()), "", t);
    }

    public ApiException(final Status status, final String message) {
        this(status, String.format("%s (%d)", status.getReasonPhrase(), status.getStatusCode()), message, null);
    }

    public static String createErrorPage(final String title, final String message, final Throwable cause) {
        final StringBuilder sb = new StringBuilder();

        sb.append("<html><body style='font: 10pt sans-serif'><h1>")
                .append(title)
                .append("</h1>")
                .append("<p class='message'>")
                .append(message)
                .append("</p>");

        if (cause != null) {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            cause.printStackTrace(pw);

            sb.append("<details><summary>")
                    .append(cause.getClass().getSimpleName())
                    .append(": ")
                    .append(cause.getMessage())
                    .append("</summary><pre style='color: gray'>")
                    .append(sw.toString())
                    .append("</pre></details>");
        }

        sb.append("</body></html>");

        return sb.toString();
    }

}
