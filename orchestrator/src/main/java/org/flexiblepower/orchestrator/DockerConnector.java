/**
 * File DockerConnector.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator;

import java.util.List;

import org.flexiblepower.model.Process;

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

    public static DockerClient init() throws DockerCertificateException {
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
    public List<Process> listProcesses() {
        // TODO Auto-generated method stub
        try {
            DockerConnector.init().listContainers();
        } catch (DockerException | InterruptedException | DockerCertificateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    /**
     * @param json
     * @return
     */
    public String newProcess(final Process process) {
        try {
            DockerConnector.init().createContainer(null);
        } catch (DockerException | InterruptedException | DockerCertificateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    /**
     * @param uuid
     * @return
     */
    public Process getProcess(final String uuid) {
        try {
            DockerConnector.init().inspectContainer(uuid);
        } catch (DockerException | InterruptedException | DockerCertificateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    /**
     * @param uuid
     * @return
     */
    public void removeProcess(final String uuid) {

        try {
            DockerConnector.init().removeContainer(uuid);
        } catch (DockerException | InterruptedException | DockerCertificateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
