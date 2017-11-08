/**
 * File DockerConnectorTest.java
 *
 * Copyright 2017 FAN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flexiblepower.orchestrator;

import java.util.List;

import org.flexiblepower.connectors.DockerConnector;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.swarm.Node;

/**
 * DockerConnectorTest
 *
 * @version 0.1
 * @since May 8, 2017
 */
@SuppressWarnings("static-method")
public class DockerConnectorTest {

    @Test
    public void runDockerConnectorTest() throws DockerException, InterruptedException, DockerCertificateException {
        try {
            DockerConnector.init().inspectSwarm();
        } catch (final Exception e) {
            Assume.assumeNoException(e);
        }

        final List<Node> nodes = DockerConnector.init().listNodes();
        System.out.println(nodes);
        Assert.assertNotNull(nodes);
    }

}
