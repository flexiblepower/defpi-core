package org.flexiblepower.defpi.dashboard;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPRequest;
import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPRequest.Method;
import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

public class HttpStaticContentHandler implements HttpHandler {

	private final static Logger LOG = LoggerFactory.getLogger(HttpStaticContentHandler.class);

	@Override
	public HTTPResponse handle(HTTPRequest request) {
		String filename = "/static/" + HttpUtils.path(request.getUri());
		File file = new File(filename);
		if (!file.exists() || !file.isFile()) {
			return HttpUtils.notFound(request);
		}
		try {
			if (!request.getMethod().equals(Method.GET)) {
				return HttpUtils.badRequest(request,
						"Method" + request.getMethod().toString() + " not allowed on this location");
			}
			FileInputStream inputStream = new FileInputStream(file);
			ByteString body = ByteString.copyFrom(IOUtils.toByteArray(inputStream));
			inputStream.close();
			return HTTPResponse.newBuilder().setId(request.getId()).setStatus(200)
					.putHeaders(HttpUtils.CONTENT_TYPE, HttpUtils.getContentType(filename)).setBody(body).build();
		} catch (IOException e) {
			LOG.error("Could not serve static content " + request.getUri(), e);
			return HttpUtils.internalError(request);
		}
	}

}
