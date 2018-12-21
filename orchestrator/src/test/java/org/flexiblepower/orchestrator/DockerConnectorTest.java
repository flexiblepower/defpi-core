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

import java.util.List;

import org.flexiblepower.connectors.DockerConnector;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.swarm.Node;

/**
 * DockerConnectorTest
 *
 * @version 0.1
 * @since May 8, 2017
 */
@SuppressWarnings({"static-method", "javadoc"})
public class DockerConnectorTest {

    @Rule
    public Timeout globalTimeout = Timeout.seconds(5);

    @Test
    public void runDockerConnectorTest() throws DockerException, InterruptedException, DockerCertificateException {
        try {
            DockerConnector.getInstance();
        } catch (final Exception e) {
            Assume.assumeNoException(e);
        }

        final List<Node> nodes = DockerConnector.getInstance().listNodes();
        System.out.println(nodes);
        Assert.assertNotNull(nodes);
    }

}
