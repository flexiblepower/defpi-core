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
import java.util.concurrent.TimeUnit;

import org.flexiblepower.connectors.DockerConnector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.swarm.Node;

/**
 * DockerConnectorTest
 *
 * @version 0.1
 * @since May 8, 2017
 */
@Disabled
@Timeout(value = 5, unit = TimeUnit.SECONDS)
@SuppressWarnings({"static-method", "javadoc"})
public class DockerConnectorTest {

    @Test
    public void runDockerConnectorTest() throws DockerException, InterruptedException, DockerCertificateException {
        try {
            DockerConnector.getInstance();
        } catch (final Exception e) {
            Assumptions.assumeTrue(false);
        }

        final List<Node> nodes = DockerConnector.getInstance().listNodes();
        System.out.println(nodes);
        Assertions.assertNotNull(nodes);
    }

}
