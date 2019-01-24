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

import org.flexiblepower.defpi.dashboard.gateway.http.proto.Gateway_httpProto.HTTPResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpRouter implements HttpHandler {

    private final static Logger LOG = LoggerFactory.getLogger(HttpRouter.class);

    private final HttpStaticContentHandler staticContentHandler;

    private final WidgetManager widgetManager;

    public HttpRouter(final WidgetManager widgetManager) {
        this.widgetManager = widgetManager;
        this.staticContentHandler = new HttpStaticContentHandler();
    }

    @Override
    public void handle(final HttpTask httpTask) {
        HttpRouter.LOG.info(httpTask.getRequest().getMethod() + ": " + httpTask.getRequest().getUri());

        // Rewrite?
        if (httpTask.getPath().equals("/")) {
            httpTask.respond(HTTPResponse.newBuilder()
                    .setId(httpTask.getRequest().getId())
                    .putHeaders("Location", "/index.html")
                    .setStatus(301)
                    .build());
            return;
        }

        // Dynamic?
        this.widgetManager.handle(new HttpTask(httpTask.getRequest(), (httpTask1, response) -> {
            // If the dynamic handler could not handle the request, try to serve static
            // content
            if (response.getStatus() == 404) {
                HttpRouter.this.staticContentHandler.handle(httpTask1.getOriginalTask());
            } else {
                // Just answer
                httpTask1.getOriginalTask().respond(response);
            }
        }, httpTask));
    }

}
