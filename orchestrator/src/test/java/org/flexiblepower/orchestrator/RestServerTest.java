/**
 * File TestSwaggerDef.java
 *
 * Copyright 2017 FAN
 */
package org.flexiblepower.orchestrator;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

import javax.ws.rs.core.MediaType;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
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
import org.flexiblepower.rest.OrchestratorApplication;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.SwaggerDefinition;

/**
 * TestSwaggerDef
 *
 * @version 0.1
 * @since Nov 23, 2017
 */
@SuppressWarnings("static-method")
public class RestServerTest {

    private static final int TEST_PORT = Main.URI_PORT;
    private static Server server;

    @BeforeClass
    public static void init() throws Exception {
        RestServerTest.server = Main.startServer();
    }

    @AfterClass
    public static void stop() throws Exception {
        RestServerTest.server.stop();
    }

    @Rule
    public Timeout globalTimeout = Timeout.seconds(10);

    @Test
    public void testListUsers() throws ClientProtocolException, URISyntaxException, IOException {
        this.defaultTests("user", "_perPage=3&_sortDir=DESC", 200, MediaType.APPLICATION_JSON_TYPE);
    }

    @Test
    public void testListProcesses() throws ClientProtocolException, URISyntaxException, IOException {
        System.out.println(this.defaultTests("process",
                "_page=0&_perPage=0&_sortDir=&_sortField=",
                200,
                MediaType.APPLICATION_JSON_TYPE));
    }

    @Test
    public void test404() throws ClientProtocolException, URISyntaxException, IOException {
        this.defaultTests("bestaatniet", null, 404, MediaType.APPLICATION_JSON_TYPE);
    }

    @Test
    public void testSwagger() throws Exception {
        final String content = this.defaultTests("swagger.json", null, 200, MediaType.APPLICATION_JSON_TYPE);

        Assert.assertTrue(content.contains("swagger"));

        Assert.assertTrue(content
                .contains(OrchestratorApplication.class.getAnnotation(SwaggerDefinition.class).info().description()));
        Assert.assertTrue(content.contains(
                OrchestratorApplication.class.getAnnotation(SwaggerDefinition.class).info().termsOfService()));

        for (final Class<?> c : new Class[] {ConnectionApi.class, InterfaceApi.class, NodeApi.class,
                PendingChangeApi.class, ProcessApi.class, ServiceApi.class, UserApi.class}) {
            for (final Method m : c.getMethods()) {
                final ApiOperation annotation = m.getAnnotation(ApiOperation.class);
                Assert.assertNotNull(
                        "Method " + m.getName() + " of class " + c.getName()
                                + " does not contain the @ApiOperation annotation",
                        annotation);
                Assert.assertTrue(content.contains(annotation.value()));
                Assert.assertTrue(content.contains(annotation.notes()));
                Assert.assertTrue(content.contains(annotation.nickname()));
            }
        }
    }

    @SuppressWarnings("resource")
    private String defaultTests(final String path,
            final String query,
            final int expectedResponse,
            final MediaType expectedType) throws URISyntaxException,
            ClientProtocolException,
            IOException {
        final URI uri = new URI("http",
                null,
                InetAddress.getLocalHost().getHostName(),
                RestServerTest.TEST_PORT,
                Main.URI_PATH + "/" + path,
                query,
                null);

        System.out.println("GETting " + uri);
        final HttpGet request = new HttpGet(uri);
        final String auth = "admin:admin";
        final byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("UTF-8")));
        request.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + new String(encodedAuth));
        final HttpClient client = HttpClientBuilder.create().build();

        final HttpResponse response = client.execute(request);
        Assert.assertEquals(expectedResponse, response.getStatusLine().getStatusCode());
        if (response.getEntity().getContentLength() == 0) {
            return "";
        }
        Assert.assertEquals(expectedType.toString(), response.getEntity().getContentType().getValue());

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
