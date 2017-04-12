/**
 * File DockerConnector.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.orchestrator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flexiblepower.exceptions.ApiException;
import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.UnidentifiedNode;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.exceptions.ServiceNotFoundException;
import com.spotify.docker.client.messages.swarm.ContainerSpec;
import com.spotify.docker.client.messages.swarm.Placement;
import com.spotify.docker.client.messages.swarm.Service;
import com.spotify.docker.client.messages.swarm.ServiceSpec;
import com.spotify.docker.client.messages.swarm.TaskSpec;

import lombok.extern.slf4j.Slf4j;

/**
 * DockerConnector
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 20 mrt. 2017
 */
@Slf4j
public class DockerConnector {

    // private static final String CERT_PATH = "C:\\Users\\leeuwencjv\\.docker\\machine\\machines\\default";
    private static final String DOCKER_HOST_KEY = "DOCKER_HOST";
    private static final String USER_LABEL_KEY = "user";

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
        final List<Process> ret = new ArrayList<>();

        try {
            final List<Service> serviceList = DockerConnector.init().listServices();
            serviceList.forEach((service) -> ret.add(DockerConnector.service2Process(service)));
        } catch (DockerException | InterruptedException | DockerCertificateException e) {
            throw new ApiException(e);
        }

        return ret;
    }

    /**
     * @param json
     * @return
     */
    public String newProcess(final Process process) {
        try {
            return DockerConnector.init().createService(DockerConnector.process2ServiceSpec(process)).id();
        } catch (DockerException | InterruptedException | DockerCertificateException e) {
            DockerConnector.log.error("Error while creating process: {}", e.getMessage());
            DockerConnector.log.trace("Error while creating process", e);
            throw new ApiException(e);
        }
    }

    /**
     * @param uuid
     * @return
     * @throws ProcessNotFoundException
     */
    public Process getProcess(final String uuid) throws ProcessNotFoundException {
        try {
            final Service service = DockerConnector.init().inspectService(uuid);
            return DockerConnector.service2Process(service);
        } catch (final ServiceNotFoundException e) {
            DockerConnector.log.error("Could not find process with id {]", uuid);
            throw new ProcessNotFoundException(uuid);
        } catch (DockerException | InterruptedException | DockerCertificateException e) {
            DockerConnector.log.error("Error while getting process: {}", e.getMessage());
            DockerConnector.log.trace("Error while getting process", e);
            throw new ApiException(e);
        }
    }

    /**
     * @param service
     * @return
     */
    static Process service2Process(final Service service) {
        final Process process = new Process();
        process.setId(service.id());

        final Map<String, String> serviceLabels = service.spec().labels();
        if (serviceLabels != null) {
            process.setUserName(serviceLabels.get(DockerConnector.USER_LABEL_KEY));
        }
        process.setRunningNode((new UnidentifiedNode("NOT DEFINED YET")));

        final org.flexiblepower.model.Service processService = new org.flexiblepower.model.Service();
        processService.setName(service.spec().name());
        processService.setFullImage(service.spec().taskTemplate().containerSpec().image());
        processService.setCreated(service.createdAt().toString());

        process.setProcessService(processService);

        return process;
    }

    /**
     * @param process
     * @return
     */
    static ServiceSpec process2ServiceSpec(final Process process) {
        final String dockerImage = process.getProcessService().getFullImageName();
        String serviceName = process.getUserName() + "_" + process.getProcessService().getName();
        serviceName = serviceName.replaceAll("\\h", "");

        final Map<String, String> serviceLabels = new HashMap<>();
        serviceLabels.put(DockerConnector.USER_LABEL_KEY, process.getUserName());

        final ContainerSpec processSpec = ContainerSpec.builder().image(dockerImage).build();
        final Placement placement = Placement
                .create(Arrays.asList("node.hostname == " + process.getRunningNode().getHostname()));
        final TaskSpec taskTemplate = TaskSpec.builder().containerSpec(processSpec).placement(placement).build();

        return ServiceSpec.builder().name(serviceName).labels(serviceLabels).taskTemplate(taskTemplate).build();
    }

    /**
     * @param uuid
     * @return
     */
    public void removeProcess(final String uuid) throws ProcessNotFoundException {
        try {
            DockerConnector.init().removeService(uuid);
        } catch (final ServiceNotFoundException e) {
            DockerConnector.log.error("Could not find process with id {]", uuid);
            throw new ProcessNotFoundException(uuid);
        } catch (DockerException | InterruptedException | DockerCertificateException e) {
            DockerConnector.log.error("Error while removing process: {}", e.getMessage());
            DockerConnector.log.trace("Error while removing process", e);
            throw new ApiException(e);
        }
    }

}
