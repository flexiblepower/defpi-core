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
import com.spotify.docker.client.messages.Network;
import com.spotify.docker.client.messages.NetworkConfig;
import com.spotify.docker.client.messages.swarm.ContainerSpec;
import com.spotify.docker.client.messages.swarm.EndpointSpec;
import com.spotify.docker.client.messages.swarm.EndpointSpec.Builder;
import com.spotify.docker.client.messages.swarm.NetworkAttachmentConfig;
import com.spotify.docker.client.messages.swarm.Placement;
import com.spotify.docker.client.messages.swarm.PortConfig;
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

    private final DockerClient client;

    public static DockerClient init() throws DockerCertificateException {
        final String dockerHost = System.getenv(DockerConnector.DOCKER_HOST_KEY);
        if (dockerHost == null) {
            return DefaultDockerClient.fromEnv().build();
        } else {
            return DefaultDockerClient.builder().uri(dockerHost).build();
        }
        // .dockerCertificates(new DockerCertificates(Paths.get(DockerConnector.CERT_PATH)))
    }

    public DockerConnector() {
        try {
            this.client = DockerConnector.init();
        } catch (final DockerCertificateException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return
     */
    public List<Process> listProcesses() {
        final List<Process> ret = new ArrayList<>();

        try {
            final List<Service> serviceList = this.client.listServices();
            serviceList.forEach((service) -> ret.add(DockerConnector.dockerService2Process(service)));
        } catch (DockerException | InterruptedException e) {
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
            return this.client.createService(DockerConnector.process2ServiceSpec(process)).id();
        } catch (DockerException | InterruptedException e) {
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
            final Service service = this.client.inspectService(uuid);
            return DockerConnector.dockerService2Process(service);
        } catch (final ServiceNotFoundException e) {
            DockerConnector.log.error("Could not find process with id {]", uuid);
            throw new ProcessNotFoundException(uuid);
        } catch (DockerException | InterruptedException e) {
            DockerConnector.log.error("Error while getting process: {}", e.getMessage());
            DockerConnector.log.trace("Error while getting process", e);
            throw new ApiException(e);
        }
    }

    /**
     * @param uuid
     * @return
     */
    public void removeProcess(final String uuid) throws ProcessNotFoundException {
        try {
            this.client.removeService(uuid);
        } catch (final ServiceNotFoundException e) {
            DockerConnector.log.error("Could not find process with id {}", uuid);
            throw new ProcessNotFoundException(uuid);
        } catch (DockerException | InterruptedException e) {
            DockerConnector.log.error("Error while removing process: {}", e.getMessage());
            DockerConnector.log.trace("Error while removing process", e);
            throw new ApiException(e);
        }
    }

    /**
     * @return
     */
    public Map<String, String> listNetworks() {
        try {
            final List<Network> networks = this.client.listNetworks();
            final Map<String, String> ret = new HashMap<>();
            networks.forEach((x) -> {
                ret.put(x.id(), x.name());
            });
            return ret;
        } catch (DockerException | InterruptedException e) {
            DockerConnector.log.error("Error while listing networks: {}", e.getMessage());
            DockerConnector.log.trace("Error while listing networks", e);
            throw new ApiException(e);
        }
    }

    /**
     * @param networkName
     * @return
     */
    public String newNetwork(final String networkName) {
        try {
            final NetworkConfig networkConfig = NetworkConfig.builder().driver("overlay").name(networkName).build();
            return this.client.createNetwork(networkConfig).id();
        } catch (DockerException | InterruptedException e) {
            DockerConnector.log.error("Error while creating new network: {}", e.getMessage());
            DockerConnector.log.trace("Error while creating new network", e);
            throw new ApiException(e);
        }
    }

    /**
     * @param networkId
     */
    public void removeNetwork(final String networkId) {
        try {
            this.client.removeNetwork(networkId);
        } catch (DockerException | InterruptedException e) {
            DockerConnector.log.error("Error while removing network: {}", e.getMessage());
            DockerConnector.log.trace("Error while removing network", e);
            throw new ApiException(e);
        }
    }

    /**
     * Internal function to translate a docker service to a def-pi process definition.
     *
     * @param service
     * @return
     */
    static Process dockerService2Process(final Service service) {
        // Create the def-pi service using the docker image information
        final org.flexiblepower.model.Service processService = new org.flexiblepower.model.Service();
        processService.setName(service.spec().name());
        processService.setFullImage(service.spec().taskTemplate().containerSpec().image());
        processService.setCreated(service.createdAt().toString());

        // Create the def-pi process
        final Process process = new Process();
        process.setId(service.id());
        process.setProcessService(processService);

        // Get additional information from the docker image labels
        final Map<String, String> serviceLabels = service.spec().labels();
        if (serviceLabels != null) {
            process.setUserName(serviceLabels.get(DockerConnector.USER_LABEL_KEY));
        }
        process.setRunningNode(new UnidentifiedNode("NOT DEFINED YET"));

        return process;
    }

    /**
     * Internal function to translate a def-pi process definition to a docker service specification
     *
     * @param process
     * @return
     */
    static ServiceSpec process2ServiceSpec(final Process process) {
        // Create a name for the service by removing blanks from process name
        String serviceName = process.getUserName() + "_" + process.getProcessService().getName();
        serviceName = serviceName.replaceAll("\\h", "");

        // Create labels to add to the container
        final Map<String, String> serviceLabels = new HashMap<>();
        serviceLabels.put(DockerConnector.USER_LABEL_KEY, process.getUserName());

        // Create the task template based on the process image
        final String dockerImage = process.getProcessService().getFullImageName();
        final ContainerSpec processSpec = ContainerSpec.builder().image(dockerImage).build();
        final Placement placement = Placement
                .create(Arrays.asList("node.hostname == " + process.getRunningNode().getHostname()));
        final TaskSpec taskTemplate = TaskSpec.builder().containerSpec(processSpec).placement(placement).build();

        // Add the ports to the specification
        final Builder endpointSpec = EndpointSpec.builder();
        for (final String port : process.getProcessService().getPorts()) {
            final int pos = port.indexOf(':');
            if (pos < 0) {
                throw new IllegalArgumentException("Port has invalid syntax, expecting port:port");
            }
            final Integer src = Integer.parseInt(port.substring(0, pos));
            final Integer dst = Integer.parseInt(port.substring(pos + 1));
            endpointSpec.addPort(PortConfig.builder().publishedPort(src).targetPort(dst).build());
        }

        // Add the network attachment to place process in user network
        final NetworkAttachmentConfig network = NetworkAttachmentConfig.builder().target("user-net").build();

        return ServiceSpec.builder()
                .name(serviceName)
                .labels(serviceLabels)
                .taskTemplate(taskTemplate)
                .endpointSpec(endpointSpec.build())
                .networks(network)
                .build();
    }

}
