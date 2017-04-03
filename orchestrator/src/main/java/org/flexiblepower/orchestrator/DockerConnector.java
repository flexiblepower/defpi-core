/**
 * File DockerConnector.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator;

import java.util.List;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;

/**
 * DockerConnector
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 20 mrt. 2017
 */
public class DockerConnector {

    // private static final String CERT_PATH = "C:\\Users\\leeuwencjv\\.docker\\machine\\machines\\default";
    private static final String DOCKER_HOST_KEY = "DOCKER_HOST";

    public static DockerClient init() throws DockerCertificateException, DockerException, InterruptedException {
        final String dockerHost = System.getenv(DockerConnector.DOCKER_HOST_KEY);
        if (dockerHost == null) {
            return DefaultDockerClient.fromEnv().build();
        } else {
            return DefaultDockerClient.builder().uri(dockerHost).build();
        }
        // .dockerCertificates(new DockerCertificates(Paths.get(DockerConnector.CERT_PATH)))
    }

    /**
     * @return
     */
    public List<Process> getProcesses() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @param json
     * @return
     */
    public String newProcess(final String json) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @param uuid
     * @return
     */
    public Process getProcess(final String uuid) {
        // TODO Auto-generated method stub
        return null;
    }

}
