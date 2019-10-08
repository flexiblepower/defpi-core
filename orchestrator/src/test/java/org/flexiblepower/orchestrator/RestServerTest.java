/*-
 * #%L
 * dEF-Pi REST Orchestrator
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
package org.flexiblepower.orchestrator;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.jetty.server.Server;
import org.flexiblepower.api.ConnectionApi;
import org.flexiblepower.api.InterfaceApi;
import org.flexiblepower.api.NodeApi;
import org.flexiblepower.api.PendingChangeApi;
import org.flexiblepower.api.ProcessApi;
import org.flexiblepower.api.ServiceApi;
import org.flexiblepower.api.UserApi;
import org.flexiblepower.connectors.DockerConnector;
import org.flexiblepower.rest.OrchestratorApplication;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.SwaggerDefinition;

/**
 * RestServerTest
 *
 * @version 0.1
 * @since Nov 23, 2017
 */
@Timeout(value = 20, unit = TimeUnit.SECONDS)
@SuppressWarnings({"static-method", "javadoc"})
public class RestServerTest {

    private static final int TEST_PORT = Main.URI_PORT;
    private static Server server;

    @BeforeAll
    public static void init() throws Exception {
        RestServerTest.server = Main.startServer();
    }

    @AfterAll
    public static void stop() throws Exception {
        RestServerTest.server.stop();
    }

    public void testListUsers() throws Exception {
        this.defaultTests("user", "_perPage=2&_sortDir=DESC", 200, MediaType.APPLICATION_JSON_TYPE);
    }

    public void testListProcesses() throws Exception {
        System.out.println(this.defaultTests("process",
                "_page=0&_perPage=0&_sortDir=&_sortField=",
                200,
                MediaType.APPLICATION_JSON_TYPE));
    }

    public void testListNodes() throws Exception {
        this.defaultTests("unidentifiednode", null, 200, MediaType.APPLICATION_JSON_TYPE);
    }

    @Test
    public void test401() throws Exception {
        final String content = this.defaultTests("service", null, 401, MediaType.TEXT_HTML_TYPE);
        Assertions.assertTrue(content.contains("<h1>Unauthorized</h1>"));
        Assertions.assertTrue(content.contains("The user is not authorized to perform this operation"));

        // We do not expect a stack trace
        Assertions.assertFalse(content.contains("<details><summary>"));
    }

    @Test
    public void test400() throws Exception {
        final String content = this.defaultTests("connection/1", null, 400, MediaType.TEXT_HTML_TYPE);
        Assertions.assertTrue(content.contains("<h1>Invalid input</h1>"));
        Assertions.assertTrue(content.contains("The provided id is not a valid ObjectId (1)"));

        // We do not expect a stack trace
        Assertions.assertFalse(content.contains("<details><summary>"));
    }

    @Test
    public void test404() throws Exception {
        this.defaultTests("bestaatniet", null, 404, MediaType.APPLICATION_JSON_TYPE);
    }

    @Test
    public void testDiag() throws Exception {
        try {
            DockerConnector.getInstance();
            this.defaultTests("diag", null, 200, MediaType.TEXT_PLAIN_TYPE);
        } catch (final Exception e) {
            // We expect the same error, but in HTML 500 format
            this.defaultTests("diag", null, 500, MediaType.TEXT_HTML_TYPE);
        }
    }

    @Test
    public void testSwagger() throws Exception {
        final String content = this.defaultTests("swagger.json", null, 200, MediaType.APPLICATION_JSON_TYPE);

        // Test for some stuff which should NOT be there
        Assertions.assertFalse(content.contains("Response"));
        Assertions.assertFalse(content.contains("NewCookie"));
        Assertions.assertFalse(content.contains("Exception"));

        Assertions.assertTrue(content.contains("swagger"));

        Assertions.assertTrue(content
                .contains(OrchestratorApplication.class.getAnnotation(SwaggerDefinition.class).info().description()));
        Assertions.assertTrue(content.contains(
                OrchestratorApplication.class.getAnnotation(SwaggerDefinition.class).info().termsOfService()));

        for (final Class<?> c : new Class[] {ConnectionApi.class, InterfaceApi.class, NodeApi.class,
                PendingChangeApi.class, ProcessApi.class, ServiceApi.class, UserApi.class}) {
            for (final Method m : c.getMethods()) {
                final ApiOperation annotation = m.getAnnotation(ApiOperation.class);
                Assertions.assertNotNull(annotation,
                        "Method " + m.getName() + " of class " + c.getName()
                                + " does not contain the @ApiOperation annotation");
                Assertions.assertTrue(content.contains(annotation.value()));
                Assertions.assertTrue(content.contains(annotation.notes()));
                Assertions.assertTrue(content.contains(annotation.nickname()));
            }
        }
    }

    @SuppressWarnings("resource")
    private String defaultTests(final String path,
            final String query,
            final int expectedResponse,
            final MediaType expectedType) throws Exception {
        final URI uri = new URI("http",
                null,
                InetAddress.getLocalHost().getHostName(),
                RestServerTest.TEST_PORT,
                "/" + path,
                query,
                null);

        final HttpGet request = new HttpGet(uri);
        final HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(request);
        Assertions.assertEquals(expectedResponse, response.getStatusLine().getStatusCode());
        if (response.getEntity().getContentLength() == 0) {
            return "";
        }
        Assertions.assertEquals(expectedType.toString(), response.getEntity().getContentType().getValue());

        final byte[] buf = new byte[128];
        int len = 0;
        final StringBuilder builder = new StringBuilder();
        try (final InputStream is = response.getEntity().getContent()) {
            while ((len = is.read(buf)) > 0) {
                builder.append(new String(buf, 0, len));
            }
        }

        return builder.toString();
    }

}
