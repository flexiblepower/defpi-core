/**
 * File DockerConnector.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.connectors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.flexiblepower.exceptions.ApiException;
import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.Architecture;
import org.flexiblepower.model.Node;
import org.flexiblepower.model.NodePool;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.PublicNode;
import org.flexiblepower.model.Service;
import org.flexiblepower.orchestrator.NodeManager;
import org.flexiblepower.orchestrator.ServiceManager;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerClient.ListContainersParam;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.Network;
import com.spotify.docker.client.messages.NetworkConfig;
import com.spotify.docker.client.messages.mount.Mount;
import com.spotify.docker.client.messages.swarm.ContainerSpec;
import com.spotify.docker.client.messages.swarm.EndpointSpec;
import com.spotify.docker.client.messages.swarm.NetworkAttachmentConfig;
import com.spotify.docker.client.messages.swarm.Placement;
import com.spotify.docker.client.messages.swarm.PortConfig;
import com.spotify.docker.client.messages.swarm.PortConfig.PortConfigPublishMode;
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

    /**
     *
     */
    private static final String ORCHESTRATOR_CONTAINER_NAME = "orchestrator_orchestrator_1";

    private static final int INTERNAL_DEBUGGING_PORT = 8000;

    private static final String DOCKER_HOST_KEY = "DOCKER_HOST";

    private static final String SERVICE_LABEL_KEY = "service.name";
    private static final String USER_LABEL_KEY = "user.id";
    private static final String NODE_ID_LABEL_KEY = "node.id";

    private static DockerConnector instance = null;

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

    private DockerConnector() {
        try {
            this.client = DockerConnector.init();
        } catch (final DockerCertificateException e) {
            throw new RuntimeException("Unable to run orchestrator without docker");
        }
    }

    public synchronized static DockerConnector getInstance() {
        if (DockerConnector.instance == null) {
            DockerConnector.instance = new DockerConnector();
        }
        return DockerConnector.instance;
    }

    /**
     * @param json
     * @return
     * @throws ServiceNotFoundException
     */
    public synchronized String newProcess(final Process process) throws ServiceNotFoundException {
        try {
            this.ensureProcessNetworkExists(process);
            this.ensureProcessNetworkIsAttached(process);

            final Service service = ServiceManager.getInstance().getService(process.getServiceId());
            final Node node = DockerConnector.determineRunningNode(process);
            final ServiceSpec serviceSpec = DockerConnector.createServiceSpec(process, service, node);
            final String id = this.client.createService(serviceSpec).id();
            DockerConnector.log.info("Created process with Id {}", id);
            return id;
        } catch (DockerException | InterruptedException e) {
            DockerConnector.log.debug("Exception while starting new process: {}", e.getMessage());
            DockerConnector.log.trace(e.getMessage(), e);
            return null;
        }
    }

    /**
     * @param uuid
     * @return
     * @throws ProcessNotFoundException
     * @throws ServiceNotFoundException
     */
    public synchronized boolean removeProcess(final Process process) throws ProcessNotFoundException {
        if (process.getDockerId() != null) {
            try {
                this.client.removeService(process.getDockerId());
                return true;
            } catch (final com.spotify.docker.client.exceptions.ServiceNotFoundException e) {
                throw new ProcessNotFoundException(process.getId().toString());
            } catch (DockerException | InterruptedException e) {
                DockerConnector.log.error("Error while removing process: {}", e.getMessage());
            }
        }
        return false;
    }

    /**
     * @return
     */
    public synchronized List<com.spotify.docker.client.messages.swarm.Node> listNodes() {
        try {
            return this.client.listNodes();
        } catch (DockerException | InterruptedException e) {
            DockerConnector.log.error("Error while listing nodes: {}", e.getMessage());
            DockerConnector.log.trace("Error while listing nodes", e);
            throw new ApiException(e);
        }
    }

    private void ensureProcessNetworkExists(final Process process) throws DockerException, InterruptedException {
        final String networkName = DockerConnector.getNetworkFromProcess(process);
        // check if it exists
        if (!this.listNetworks().values().contains(networkName)) {
            this.newNetwork(networkName);
        }
    }

    private static String getNetworkFromProcess(final Process process) {
        return "usernet-" + process.getUserId().toString();
    }

    /**
     * And connect to the new network
     *
     * @param networkName the name of the network to attach to
     * @throws InterruptedException
     * @throws DockerException
     */
    public void ensureProcessNetworkIsAttached(final Process process) throws DockerException, InterruptedException {
        final String networkName = DockerConnector.getNetworkFromProcess(process);
        final List<Container> orchestratorContainers = this.client
                .listContainers(ListContainersParam.filter("name", DockerConnector.ORCHESTRATOR_CONTAINER_NAME));
        if (!orchestratorContainers.isEmpty()) {
            // If the list is empty the orchestrator does not exist!
            if (!this.client.inspectContainer(DockerConnector.ORCHESTRATOR_CONTAINER_NAME)
                    .networkSettings()
                    .networks()
                    .containsKey(networkName)) {
                DockerConnector.log.info("Connecting {} to network {}",
                        DockerConnector.ORCHESTRATOR_CONTAINER_NAME,
                        networkName);
                this.client.connectToNetwork(DockerConnector.ORCHESTRATOR_CONTAINER_NAME, networkName);
            } else {
                DockerConnector.log.debug("Container {} is already connected to {}",
                        DockerConnector.ORCHESTRATOR_CONTAINER_NAME,
                        networkName);
            }
        } else {
            DockerConnector.log.warn("No container running with expected name {}. Not connecting to network {}.",
                    DockerConnector.ORCHESTRATOR_CONTAINER_NAME,
                    networkName);
        }

    }

    /**
     * @return
     * @throws InterruptedException
     * @throws DockerException
     */
    private Map<String, String> listNetworks() throws DockerException, InterruptedException {
        final List<Network> networks = this.client.listNetworks();
        final Map<String, String> ret = new HashMap<>();
        networks.forEach((x) -> ret.put(x.id(), x.name()));
        return ret;
    }

    /**
     * @param networkName
     * @return
     * @throws InterruptedException
     * @throws DockerException
     */
    private String newNetwork(final String networkName) throws DockerException, InterruptedException {
        // TODO: Add encrypted network? .addOption("encrypted", "")
        final NetworkConfig networkConfig = NetworkConfig.builder()
                .driver("overlay")
                .attachable(true)
                .name(networkName)
                .build();
        return this.client.createNetwork(networkConfig).id();
    }

    /**
     * @param networkId
     * @throws InterruptedException
     * @throws DockerException
     */
    private void removeNetwork(final String networkId) throws DockerException, InterruptedException {
        this.client.disconnectFromNetwork(DockerConnector.ORCHESTRATOR_CONTAINER_NAME, networkId);
        this.client.removeNetwork(networkId);
    }

    private static Node determineRunningNode(final Process process) {
        final NodeManager nm = NodeManager.getInstance();
        if (process.getNodePoolId() != null) {
            // Get node from nodepool
            final NodePool nodePool = nm.getNodePool(process.getNodePoolId());
            final List<PublicNode> nodes = nm.getPublicNodesInNodePool(nodePool);
            final Random r = new Random();
            return nodes.get(r.nextInt(nodes.size()));
        } else {
            // get Private node
            return nm.getPrivateNode(process.getPrivateNodeId());
        }
    }

    /**
     * Internal function to translate a def-pi process definition to a docker service specification
     *
     * @param process
     * @return
     */
    private static ServiceSpec createServiceSpec(final Process process, final Service service, final Node node) {

        final Architecture architecture = node.getArchitecture();
        // Create a name for the service by removing blanks from process name
        // String serviceName = service.getName() + UUID.randomUUID().getLeastSignificantBits();
        // serviceName = serviceName.replaceAll("\\h", "");
        final String serviceName = process.getId().toString();

        // Create labels to add to the container
        final Map<String, String> serviceLabels = new HashMap<>();
        serviceLabels.put(DockerConnector.SERVICE_LABEL_KEY,
                service.getRegistry() + "/" + ServiceManager.SERVICE_REPOSITORY + "/" + service.getId() + ":"
                        + service.getTags().get(architecture));

        final String dockerImage = service.getFullImageName(architecture);
        serviceLabels.put(DockerConnector.USER_LABEL_KEY, process.getUserId().toString());
        serviceLabels.put(DockerConnector.NODE_ID_LABEL_KEY, node.getDockerId());

        // Create the builders for the task template
        final TaskSpec.Builder taskSpec = TaskSpec.builder();
        final EndpointSpec.Builder endpointSpec = EndpointSpec.builder();
        final ContainerSpec.Builder containerSpec = ContainerSpec.builder().image(dockerImage);

        final Map<String, String> envArgs = new HashMap<>();
        if (process.getDebuggingPort() != 0) {
            envArgs.put("JVM_ARGUMENTS",
                    String.format("-Xdebug -agentlib:jdwp=transport=dt_socket,server=y,suspend=%s,address=%d",
                            process.isSuspendOnDebug() ? "y" : "n",
                            DockerConnector.INTERNAL_DEBUGGING_PORT));
            endpointSpec.addPort(PortConfig.builder()
                    .publishedPort(process.getDebuggingPort())
                    .targetPort(DockerConnector.INTERNAL_DEBUGGING_PORT)
                    .publishMode(PortConfigPublishMode.HOST)
                    .build());
        }

        // Add mounts to the container
        final List<Mount> mountList = new ArrayList<>();
        if (process.getMountPoints() != null) {
            for (final Entry<String, String> mount : process.getMountPoints().entrySet()) {
                mountList.add(Mount.builder().source(mount.getKey()).target(mount.getValue()).build());
            }
            containerSpec.mounts(mountList);
        }

        // Add the network attachment to place process in user network
        // final NetworkAttachmentConfig orchestratornet = NetworkAttachmentConfig.builder()
        // .target(DockerConnector.ORCHESTRATOR_NETWORK_NAME)
        // .aliases(process.getId().toString())
        // .build();

        final NetworkAttachmentConfig usernet = NetworkAttachmentConfig.builder()
                .target(DockerConnector.getNetworkFromProcess(process))
                .aliases(process.getId().toString())
                .build();

        // Add all container environment variables
        final List<String> envList = new ArrayList<>();
        envArgs.forEach((key, value) -> envList.add(key + "=" + value));
        containerSpec.env(envList);

        // Add the containerSpec and placement to the taskSpec
        final Placement placement = Placement.create(Arrays.asList("node.id == " + node.getDockerId()));
        taskSpec.containerSpec(containerSpec.build()).placement(placement);
        return ServiceSpec.builder()
                .name(serviceName)
                .labels(serviceLabels)
                .taskTemplate(taskSpec.build())
                .endpointSpec(endpointSpec.build())
                .networks(usernet)
                .build();
    }

    public String getContainerInfo() {
        try {
            final ContainerInfo info = this.client.inspectContainer(DockerConnector.ORCHESTRATOR_CONTAINER_NAME);
            return String.format("image: %s\ncreated: %s\nnetworks: %s",
                    info.image(),
                    info.created(),
                    info.networkSettings().networks().keySet());
        } catch (DockerException | InterruptedException e) {
            DockerConnector.log.warn("Error obtaining running image: {}", e.getMessage());
            DockerConnector.log.trace(e.getMessage(), e);
            return "Unknown";
        }
    }

}
