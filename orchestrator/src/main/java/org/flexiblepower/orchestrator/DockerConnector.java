/**
 * File DockerConnector.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator;

import java.net.URI;
import java.nio.file.Paths;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerCertificates;
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

    private static final String CERT_PATH = "C:\\Users\\leeuwencjv\\.docker\\machine\\machines\\default";
    private static final String DOCKER_HOST = "https://192.168.137.111:2376/";

    public static DockerClient init() throws DockerCertificateException, DockerException, InterruptedException {
        return DefaultDockerClient.builder()
                .uri(URI.create(DockerConnector.DOCKER_HOST))
                .dockerCertificates(new DockerCertificates(Paths.get(DockerConnector.CERT_PATH)))
                .build();
    }

}
