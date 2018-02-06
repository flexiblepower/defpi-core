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
import org.flexiblepower.rest.OrchestratorApplication;
import org.junit.Assert;
import org.junit.Test;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.SwaggerDefinition;

/**
 * TestSwaggerDef
 *
 * @version 0.1
 * @since Nov 23, 2017
 */
@SuppressWarnings("static-method")
public class SwaggerDefinitionTest {

    @Test(timeout = 30000)
    public void runTest() throws Exception {
        final Server x = Main.startServer();

        final URI uri = new URI("http",
                null,
                InetAddress.getLocalHost().getHostName(),
                Main.URI_PORT,
                Main.URI_PATH + "/swagger.json",
                null,
                null);

        System.out.println("GETting " + uri);
        @SuppressWarnings("resource")
        final HttpClient client = HttpClientBuilder.create().build();

        // Create http request with token in header
        final HttpGet request = new HttpGet(uri);

        final HttpResponse response = client.execute(request);
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
        Assert.assertEquals(MediaType.APPLICATION_JSON, response.getEntity().getContentType().getValue());

        final byte[] buf = new byte[128];
        int len = 0;
        final StringBuilder builder = new StringBuilder();
        try (final InputStream is = response.getEntity().getContent()) {
            while ((len = is.read(buf)) > 0) {
                builder.append(new String(buf, 0, len));
            }
        } catch (final IOException e) {
            // TODO Auto-generated catch block
        }

        final String content = builder.toString();
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

        x.stop();
    }

}
