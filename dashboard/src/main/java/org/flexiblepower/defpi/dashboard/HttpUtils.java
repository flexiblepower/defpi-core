package org.flexiblepower.defpi.dashboard;

/*-
 * #%L
 * dEF-Pi dashboard
 * %%
 * Copyright (C) 2017 - 2018 Flexible Power Alliance Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPRequest;
import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPResponse;

import com.google.protobuf.ByteString;

public class HttpUtils {

    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_JAVASCRIPT = "application/javascript";
    public static final String TEXT_PLAIN = "text/plain";
    public static final String TEXT_HTML = "text/html";
    public static final String NO_CACHE_KEY = "Cache-Control";
    public static final String NO_CACHE_VALUE = "no-cache, no-store, must-revalidate";

    public static void notFound(final HttpTask httpTask) {
        httpTask.respond(HTTPResponse.newBuilder()
                .setId(httpTask.getRequest().getId())
                .setStatus(404)
                .putHeaders(HttpUtils.CONTENT_TYPE, HttpUtils.TEXT_PLAIN)
                .setBody(ByteString.copyFromUtf8("404: Not found\nThe requested resource "
                        + httpTask.getRequest().getUri() + " could not be found"))
                .build());
    }

    public static void internalError(final HttpTask httpTask) {
        httpTask.respond(HTTPResponse.newBuilder()
                .setId(httpTask.getRequest().getId())
                .setStatus(500)
                .putHeaders(HttpUtils.CONTENT_TYPE, HttpUtils.TEXT_PLAIN)
                .setBody(ByteString.copyFromUtf8("500: Internal server error."))
                .build());
    }

    public static void internalError(final HttpTask httpTask, final String reason) {
        httpTask.respond(HTTPResponse.newBuilder()
                .setId(httpTask.getRequest().getId())
                .setStatus(500)
                .putHeaders(HttpUtils.CONTENT_TYPE, HttpUtils.TEXT_PLAIN)
                .setBody(ByteString.copyFromUtf8("500: Internal server error\n" + reason))
                .build());
    }

    public static void badRequest(final HttpTask httpTask, final String reason) {
        httpTask.respond(HTTPResponse.newBuilder()
                .setId(httpTask.getRequest().getId())
                .setStatus(400)
                .putHeaders(HttpUtils.CONTENT_TYPE, HttpUtils.TEXT_PLAIN)
                .setBody(ByteString.copyFromUtf8("400: Bad request\n" + reason))
                .build());
    }

    public static void serveStaticFile(final HttpTask httpTask, final String filename) {
        try (final FileInputStream fis = new FileInputStream(new File(filename))) {
            httpTask.respond(HTTPResponse.newBuilder()
                    .setId(httpTask.getRequest().getId())
                    .setStatus(200)
                    .putHeaders(HttpUtils.CONTENT_TYPE, HttpUtils.getContentType(filename))
                    .setBody(ByteString.copyFrom(IOUtils.toByteArray(fis)))
                    .build());
        } catch (final Exception e) {
            HttpUtils.internalError(httpTask);
        }
    }

    public static void serveDynamicText(final HttpTask httpTask, final String contentType, final String body) {
        try {
            httpTask.respond(HTTPResponse.newBuilder()
                    .setId(httpTask.getRequest().getId())
                    .setStatus(200)
                    .putHeaders(HttpUtils.CONTENT_TYPE, contentType)
                    .putHeaders(HttpUtils.NO_CACHE_KEY, HttpUtils.NO_CACHE_VALUE)
                    .setBody(ByteString.copyFromUtf8(body))
                    .build());
        } catch (final Exception e) {
            HttpUtils.internalError(httpTask);
        }
    }

    public static HTTPResponse setNoCache(final HTTPResponse response) {
        return HTTPResponse.newBuilder(response).putHeaders(HttpUtils.NO_CACHE_KEY, HttpUtils.NO_CACHE_VALUE).build();
    }

    public static HTTPRequest rewriteUri(final HTTPRequest request, final String uri) {
        return HTTPRequest.newBuilder(request).setUri(uri).build();
    }

    public static String getContentType(final String filename) {
        final String[] uri = filename.split("\\.");

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
            contentType = HttpUtils.APPLICATION_JAVASCRIPT;
            break;
        case "css":
            contentType = "text/css";
            break;
        default:
            contentType = HttpUtils.TEXT_PLAIN;
        }
        return contentType;
    }

    public static String readTextFile(final String path) throws IOException {
        try (final FileInputStream inputStream = new FileInputStream(new File(path))) {
            return IOUtils.toString(inputStream, Charset.defaultCharset());
        }
    }

}
