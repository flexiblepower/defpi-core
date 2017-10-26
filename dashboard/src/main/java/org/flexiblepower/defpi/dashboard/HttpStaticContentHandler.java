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
		String uri = request.getUri();
		if (uri.equals("/")) {
			uri = "/index.html";
		}
		String filename = "/static/" + uri;
		File file = new File(filename);
		if (!file.exists() || !file.isFile()) {
			return HttpUtil.notFound(request);
		}
		try {
			if (!request.getMethod().equals(Method.GET)) {
				return HttpUtil.badRequest(request,
						"Method" + request.getMethod().toString() + " not allowed on this location");
			}
			FileInputStream inputStream = new FileInputStream(file);
			ByteString body = ByteString.copyFrom(IOUtils.toByteArray(inputStream));
			inputStream.close();
			return HTTPResponse.newBuilder().setId(request.getId()).setStatus(200)
					.putHeaders(HttpUtil.CONTENT_TYPE, HttpUtil.getContentType(filename)).setBody(body).build();
		} catch (IOException e) {
			LOG.error("Could not serve static content " + request.getUri(), e);
			return HttpUtil.internalError(request);
		}
	}

}
