/**
 * File DockerConnector.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator;

import java.net.URI;
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
    private static final String DOCKER_HOST = "http://192.168.239.128:2376/";

    public static DockerClient init() throws DockerCertificateException, DockerException, InterruptedException {
        return DefaultDockerClient.builder().uri(URI.create(DockerConnector.DOCKER_HOST)).build();
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
