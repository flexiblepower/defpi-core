/**
 * File DockerConnectorTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.swarm.Node;

/**
 * DockerConnectorTest
 *
 * @author coenvl
 * @version 0.1
 * @since May 8, 2017
 */
public class DockerConnectorTest {

    @Test
    public void runDockerConnectorTest() throws DockerException, InterruptedException, DockerCertificateException {
        final List<Node> nodes = DockerConnector.init().listNodes();
        System.out.println(nodes);
        Assert.assertNotNull(nodes);
    }

}
