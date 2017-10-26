package org.flexiblepower.defpi.dashboard;

import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPRequest;
import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPResponse;

import com.google.protobuf.ByteString;

public class HttpUtils {

	public static final String CONTENT_TYPE = "Content-Type";
	public static final String APPLICATION_JAVASCRIPT = "application/javascript";
	public static final String TEXT_PLAIN = "text/plain";
	public static final String NO_CACHE_KEY = "Cache-Control";
	public static final String NO_CACHE_VALUE = "no-cache, no-store, must-revalidate";

	public static HTTPResponse notFound(HTTPRequest request) {
		return HTTPResponse.newBuilder().setId(request.getId()).setStatus(404).putHeaders(CONTENT_TYPE, TEXT_PLAIN)
				.setBody(ByteString.copyFromUtf8(
						"404: Not found\nThe requested resource " + request.getUri() + " could not been found"))
				.build();
	}

	public static HTTPResponse internalError(HTTPRequest request) {
		return HTTPResponse.newBuilder().setId(request.getId()).setStatus(500).putHeaders(CONTENT_TYPE, TEXT_PLAIN)
				.setBody(ByteString.copyFromUtf8("500: Internal server error.")).build();
	}

	public static HTTPResponse internalError(HTTPRequest request, String reason) {
		return HTTPResponse.newBuilder().setId(request.getId()).setStatus(500).putHeaders(CONTENT_TYPE, TEXT_PLAIN)
				.setBody(ByteString.copyFromUtf8("500: Internal server error\n" + reason)).build();
	}

	public static HTTPResponse badRequest(HTTPRequest request, String reason) {
		return HTTPResponse.newBuilder().setId(request.getId()).setStatus(400).putHeaders(CONTENT_TYPE, TEXT_PLAIN)
				.setBody(ByteString.copyFromUtf8("400: Bad request\n" + reason)).build();
	}

	public static HTTPResponse.Builder setNoCache(HTTPResponse.Builder builder) {
		return builder.putHeaders(NO_CACHE_KEY, NO_CACHE_VALUE);
	}

	public static HTTPRequest rewriteUri(HTTPRequest request, String uri) {
		return HTTPRequest.newBuilder(request).setUri(uri).build();
	}

	public static String getContentType(String filename) {
		String[] uri = filename.split("\\.");

		String contentType;
		switch (uri[uri.length - 1].toLowerCase()) {
		case "html":
			contentType = "text/html";
			break;
		case "png":
			contentType = "image/png";
			break;
		case "jpg":
			contentType = "image/jpeg";
			break;
		case "jpeg":
			contentType = "image/jpeg";
			break;
		case "gif":
			contentType = "image/gif";
			break;
		case "js":
			contentType = APPLICATION_JAVASCRIPT;
			break;
		case "css":
			contentType = "text/css";
			break;
		default:
			contentType = TEXT_PLAIN;
		}
		return contentType;
	}

}
