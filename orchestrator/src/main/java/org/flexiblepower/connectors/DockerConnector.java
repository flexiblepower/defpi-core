/*
 * File DockerConnector.java
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
package org.flexiblepower.connectors;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.spotify.docker.client.messages.*;
import com.spotify.docker.client.messages.Network;
import com.spotify.docker.client.messages.swarm.*;
import org.flexiblepower.exceptions.ApiException;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.Architecture;
import org.flexiblepower.model.Node;
import org.flexiblepower.model.NodePool;
import org.flexiblepower.model.Process;
import org.flexiblepower.model.Process.ExposePort;
import org.flexiblepower.model.Process.MountPoint;
import org.flexiblepower.model.PublicNode;
import org.flexiblepower.model.Service;
import org.flexiblepower.model.User;
import org.flexiblepower.orchestrator.Main;
import org.flexiblepower.orchestrator.NodeManager;
import org.flexiblepower.orchestrator.ServiceManager;
import org.flexiblepower.orchestrator.UserManager;
import org.flexiblepower.process.ProcessManager;
import org.flexiblepower.proto.DefPiParams;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerClient.ListNetworksParam;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.mount.Mount;
import com.spotify.docker.client.messages.swarm.PortConfig.PortConfigPublishMode;
import com.spotify.docker.client.messages.swarm.Task.Criteria;

import lombok.extern.slf4j.Slf4j;

/**
 * DockerConnector
 *
 * @version 0.1
 * @since 20 mrt. 2017
 */
@Slf4j
public class DockerConnector {

    /*
     * Settings to be used by the docker client itself.
     */
    private static final String DOCKER_HOST_KEY = "DOCKER_HOST";
    private static final int DOCKER_WRITE_TIMEOUT_MILLIS = 35000;
    private static final long DOCKER_CLIENT_TIMEOUT = 30000;

    /*
     * The following keys are environment variables that are put in the docker file by the maven build plugin
     */
    private static final String BUILD_TIMESTAMP_KEY = "BUILD_TIMESTAMP";
    private static final String BUILD_USER_KEY = "BUILD_USER";
    private static final String GIT_BRANCH = "GIT_BRANCH";
    private static final String GIT_COMMIT = "GIT_COMMIT";
    private static final String GIT_LOG = "GIT_LOG";

    /*
     * The following labels are added to the user processes that are built by the orchestrator
     */
    private static final String SERVICE_LABEL_KEY = "service.name";
    private static final String USER_LABEL_KEY = "user.id";
    private static final String NODE_ID_LABEL_KEY = "node.id";

    private static final int INTERNAL_DEBUGGING_PORT = 8000;

    private static DockerConnector instance = null;

    // private final Map<ObjectId, Object> netLocks = new ConcurrentHashMap<>();
    // private final Object createNetLock = new Object();
    private DockerClient client;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private static int nodePicker = 0;

    private static DockerClient init() throws DockerCertificateException {
        final String dockerHost = System.getenv(DockerConnector.DOCKER_HOST_KEY);
        if (dockerHost == null) {
            return DefaultDockerClient.fromEnv()
                    .readTimeoutMillis(DockerConnector.DOCKER_CLIENT_TIMEOUT)
                    .connectTimeoutMillis(DockerConnector.DOCKER_CLIENT_TIMEOUT)
                    .build();
        } else {
            return DefaultDockerClient.builder()
                    .uri(dockerHost)
                    .readTimeoutMillis(DockerConnector.DOCKER_CLIENT_TIMEOUT)
                    .connectTimeoutMillis(DockerConnector.DOCKER_CLIENT_TIMEOUT)
                    .build();
        }
    }

    private DockerConnector() {
        try {
            this.client = DockerConnector.init();
        } catch (final DockerCertificateException e) {
            throw new RuntimeException("Unable to run orchestrator without docker");
        }
    }

    /**
     * Private destructor, is only closed after docker exception to make sure we will make a new client
     */
    private static synchronized void destroy(final String msg, final Throwable cause) {
        // this.client.close();
        DockerConnector.log.error("{}: {}", msg, cause.getMessage());
        DockerConnector.log.trace(cause.getMessage(), cause);
        // DockerConnector.instance = null;
    }

    /**
     * @return The singleton instance of the DockerConnector
     */
    public static DockerConnector getInstance() {
        if (DockerConnector.instance == null) {
            DockerConnector.instance = new DockerConnector();
        }
        return DockerConnector.instance;
    }

    /**
     * @param process The process to create a new docker service for
     * @return The id of the docker service that is created
     * @throws ServiceNotFoundException When the docker image cannot be found
     */
    public String newProcess(final Process process) throws ServiceNotFoundException {
        try {
            final List<com.spotify.docker.client.messages.swarm.Service> serviceList = this.runOrTimeout(
                    () -> this.client.listServices(com.spotify.docker.client.messages.swarm.Service.Criteria.builder()
                            .serviceName(DockerConnector.getDockerServiceNameForProcess(process))
                            .build()));
            if (!serviceList.isEmpty()) {
                // It already exists! Apparently we are success
                return serviceList.get(0).id();
            }

            // if (!this.netLocks.containsKey(process.getUserId())) {
            // this.netLocks.put(process.getUserId(), new Object());
            // }
            // // Synchronize by user to avoid many deadlocks
            // synchronized (this.netLocks.get(process.getUserId())) {
            this.ensureProcessNetworkExists(process);
            this.ensureProcessNetworkIsAttached(process);
            // }

            final Service service = ServiceManager.getInstance().getService(process.getServiceId());
            final Node node = DockerConnector.determineRunningNode(process);
            process.setRunningNodeId(node.getId());

            ServiceSpec serviceSpec;
            if (process.getServiceId().equals(ProcessManager.getDashboardGatewayServiceId())) {
                serviceSpec = this.createDashBoardGatewayProcess(process, service, node);
            } else {
                serviceSpec = DockerConnector.createServiceSpec(process, service, node);
            }
            final String id = this.runOrTimeout(() -> this.client.createService(serviceSpec).id());
            DockerConnector.log.info("Created process with Id {}", id);

            return id;
        } catch (DockerException | InterruptedException e) {
            DockerConnector.destroy("Exception while starting new process", e);
            return null;
        }
    }

    private ServiceSpec
            createDashBoardGatewayProcess(final Process process, final Service service, final Node randomNode)
                    throws DockerException,
                    InterruptedException {
        // This is the dashboard, it should be added to all user networks
        final List<String> networks = new ArrayList<>();

        final String dashboardNodeName = System.getenv(ProcessManager.DASHBOARD_GATEWAY_HOSTNAME_KEY);
        Node targetNode = randomNode;
        if (dashboardNodeName == null) {
            DockerConnector.log.warn(
                    "No dashboard gateway host is specified, running on {}."
                            + " To alter this behavior specify the system environment variable {}",
                    randomNode.getHostname(),
                    ProcessManager.DASHBOARD_GATEWAY_HOSTNAME_KEY);
        } else {
            final Node manuallySpecifiedNode = NodeManager.getInstance().getNodeByHostname(dashboardNodeName);
            if (manuallySpecifiedNode == null) {
                DockerConnector.log.warn(
                        "Could not find node with hostname %s, instead dashboard gateway will run on {}.",
                        dashboardNodeName,
                        randomNode.getHostname());
            } else {
                targetNode = manuallySpecifiedNode;
            }
        }

        for (final User u : UserManager.getInstance().getUsers()) {
            this.ensureUserNetworkExists(u);
            networks.add(DockerConnector.getNetworkNameFromUser(u));
        }

        return DockerConnector.createServiceSpec(process, service, targetNode, networks);
    }

    /**
     * @param process The process to remove
     * @return Whether the docker service is succesfully removed, or more precise, if by the end of calling this
     *         function the service is gone
     */
    public boolean removeProcess(final Process process) {
        if (process.getDockerId() == null) {
            // Container was probably never created
            return true;
        }

        try {
            return this.runOrTimeout(() -> {
                this.client.removeService(process.getDockerId());
                return true;
            });
        } catch (final InterruptedException | DockerException e) {
            DockerConnector.destroy("Error while removing process", e);
        }

        return false;
    }

    /**
     * @return A list of docker nodes in the swarm
     */
    public List<com.spotify.docker.client.messages.swarm.Node> listNodes() {
        try {
            return this.runOrTimeout(() -> this.client.listNodes());
        } catch (DockerException | InterruptedException e) {
            DockerConnector.destroy("Error while listing nodes", e);
            throw new ApiException(e);
        }
    }

    private void ensureProcessNetworkExists(final Process process) throws DockerException, InterruptedException {
        final String networkName = DockerConnector.getNetworkNameFromProcess(process);
        final List<Network> networks = this
                .runOrTimeout(() -> this.client.listNetworks(ListNetworksParam.byNetworkName(networkName)));

        // check if it exists
        if (networks.isEmpty()) {
            this.newNetwork(networkName);
        }
    }

    private void ensureUserNetworkExists(final User user) throws DockerException, InterruptedException {
        final String networkName = DockerConnector.getNetworkNameFromUser(user);
        final List<Network> networks = this
                .runOrTimeout(() -> this.client.listNetworks(ListNetworksParam.byNetworkName(networkName)));

        // check if it exists
        if (networks.isEmpty()) {
            this.newNetwork(networkName);
        }
    }

    private static String getNetworkNameFromProcess(final Process process) {
        return "usernet-" + process.getUserId().toString();
    }

    private static String getNetworkNameFromUser(final User user) {
        return "usernet-" + user.getId().toString();
    }

    /**
     * Make sure the process and the orchestrator share a network. The process belongs to a user, who has a private
     * network, and the orchestrator is added to this network in runtime.
     *
     * @param process the process which we want to make sure is in an attached network
     * @throws InterruptedException If an interruption occurs before the docker client was able to get the required info
     * @throws DockerException If an exception occurs in the docker client
     */
    void ensureProcessNetworkIsAttached(final Process process) throws DockerException, InterruptedException {
        try {
            final String newProcessNetworkName = DockerConnector.getNetworkNameFromProcess(process);
            final String networkId = this.runOrTimeout(
                    () -> this.client.listNetworks(ListNetworksParam.byNetworkName(newProcessNetworkName)).get(0).id());

            // Connect orchestrator to network
            final String orchestratorContainerId = DockerConnector.getOrchestratorContainerId();
            final ContainerInfo orchestratorInfo = this.client.inspectContainer(orchestratorContainerId);

            final Map<String, AttachedNetwork> networks = orchestratorInfo.networkSettings().networks();

            if (networks == null || networks.isEmpty() || !networks.containsKey(newProcessNetworkName)) {
                DockerConnector.log.info("Connecting {} to network {}", orchestratorContainerId, newProcessNetworkName);
                this.runOrTimeout(() -> {
                    this.client.connectToNetwork(orchestratorContainerId, newProcessNetworkName);
                    return true;
                });
            }

            // Connect dashboard gateway to network
            final Process dashboardGateway = ProcessManager.getInstance().getDashboardGateway();
            if (dashboardGateway != null) {
                final String dockerServiceId = dashboardGateway.getDockerId();
                if (!dashboardGateway.getId().equals(process.getId()) && (dockerServiceId != null)) {
                    final com.spotify.docker.client.messages.swarm.Service dashboardInfo = this.client
                            .inspectService(dockerServiceId);
                    List<NetworkAttachmentConfig> dashNets = dashboardInfo.spec().networks();
                    if (dashNets != null) {
                        for (final NetworkAttachmentConfig network : dashNets) {
                            if (networkId.equals(network.target())) {
                                // It is already attached, we're done here!
                                return;
                            }
                        }
                    }
                    // If we're here that means that the dashboard proxy is not yet part of this network
                    this.updateDashboardGatewayService(process, dashboardGateway);
                }
            }
        } catch (DockerException | InterruptedException e) {
            DockerConnector.destroy("Exception attaching process", e);
            throw e;
        }
    }

    private void updateDashboardGatewayService(final Process process, final Process dashboardGateway)
            throws DockerException,
            InterruptedException {
        final List<String> networks = new ArrayList<>();
        for (final User u : UserManager.getInstance().getUsers()) {
            networks.add(DockerConnector.getNetworkNameFromUser(u));
        }

        this.runOrTimeout(() -> {
            this.client.removeService(dashboardGateway.getDockerId());
            return true;
        });

        final ServiceCreateResponse newId = this
                .runOrTimeout(() -> this.client.createService(DockerConnector.createServiceSpec(dashboardGateway,
                        ServiceManager.getInstance().getService(dashboardGateway.getServiceId()),
                        DockerConnector.determineRunningNode(process),
                        networks)));

        dashboardGateway.setDockerId(newId.id());
        MongoDbConnector.getInstance().save(dashboardGateway);
    }

    /**
     * Get the container id of the current container by getting the local hostname.
     *
     * @return The name of the orchestrator container
     * @throws DockerException If an exception occurred by the DockerClient
     * @see InetAddress#getHostName()
     */
    private static String getOrchestratorContainerId() throws DockerException {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (final UnknownHostException e) {
            throw new DockerException("Unable to get local container id by hostname", e);
        }
    }

    /**
     * Create a new docker overlay network for a user
     *
     * @param networkName The name of the network to create
     * @throws InterruptedException When the docker operation took too long
     * @throws DockerException If an exception occurred by the DockerClient
     */
    private void newNetwork(final String networkName) throws DockerException, InterruptedException {
        // TODO: Add encrypted network? .addOption("encrypted", "")
        final NetworkConfig networkConfig = NetworkConfig.builder()
                .driver("overlay")
                .attachable(true)
                .name(networkName)
                .build();
        String netId = this.runOrTimeout(() -> this.client.createNetwork(networkConfig).id());
        log.debug("Created network with id {}", netId);
    }

    /**
     * Disconnect and remove and existing user network
     *
     * @param networkName The name of the network to remove
     * @throws InterruptedException When the docker operation took too long
     * @throws DockerException If an exception occurred by the DockerClient
     */
    @SuppressWarnings("unused")
    private void removeNetwork(final String networkName) throws DockerException, InterruptedException {
        this.runOrTimeout(() -> {
            this.client.disconnectFromNetwork(DockerConnector.getOrchestratorContainerId(), networkName);
            return true;
        });

        this.runOrTimeout(() -> {
            this.client.removeNetwork(networkName);
            return true;
        });
    }

    private static Node determineRunningNode(final Process process) {
        final NodeManager nm = NodeManager.getInstance();
        if (process.getNodePoolId() != null) {
            // Get node from nodepool
            final NodePool nodePool = nm.getNodePool(process.getNodePoolId());
            final List<PublicNode> nodes = nm.getPublicNodesInNodePool(nodePool);

            // First try to find any process that is already running on one of these nodes
            final List<Process> otherProcesses = ProcessManager.getInstance()
                    .listProcessesForUser(UserManager.getInstance().getUser(process.getUserId()));
            for (final Process p : otherProcesses) {
                // First check if the process HAS a running node
                if (p.getRunningNodeId() != null) {
                    final Node otherNode = nm.getPublicNode(p.getRunningNodeId());
                    if ((otherNode != null) && nodes.contains(otherNode)) {
                        return otherNode;
                    }
                }
            }

            // If no other process running on a node, do round-robin on all nodes
            return nodes.get(DockerConnector.nodePicker++ % nodes.size());
        } else {
            // get Private node
            return nm.getPrivateNode(process.getPrivateNodeId());
        }
    }

    /**
     * Internal function to translate a def-pi process definition to a docker service specification
     *
     * @param process The process to create the service specification for
     * @param service The service to use
     * @param node The node to run the process on
     * @return The DockerClient serviceSpec that can be instantiated for this process
     */
    private static ServiceSpec createServiceSpec(final Process process, final Service service, final Node node) {
        return DockerConnector.createServiceSpec(process,
                service,
                node,
                Collections.singletonList(DockerConnector.getNetworkNameFromProcess(process)));
    }

    /**
     * Internal function to translate a def-pi process definition to a docker service specification
     *
     * @param process The process to create the service specification for
     * @param service The service to use
     * @param node The node to run the process on
     * @param networks A list of networks to attach the process to
     * @return The DockerClient serviceSpec that can be instantiated for this process
     */
    private static ServiceSpec createServiceSpec(final Process process,
            final Service service,
            final Node node,
            final List<String> networks) {

        final Architecture architecture = node.getArchitecture();
        final String serviceName = DockerConnector.getDockerServiceNameForProcess(process);

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

        // Set JVM arguments for the container
        String jvmArgs = "-XX:MaxHeapFreeRatio=10 -XX:MinHeapFreeRatio=10 ";

        final Map<String, String> envArgs = new HashMap<>();
        if (process.getDebuggingPort() != 0) {
            jvmArgs += String.format("-Xdebug -agentlib:jdwp=transport=dt_socket,server=y,suspend=%s,address=%d",
                    process.isSuspendOnDebug() ? "y" : "n",
                    DockerConnector.INTERNAL_DEBUGGING_PORT);
            endpointSpec.addPort(PortConfig.builder()
                    .publishedPort(process.getDebuggingPort())
                    .targetPort(DockerConnector.INTERNAL_DEBUGGING_PORT)
                    .publishMode(PortConfigPublishMode.HOST)
                    .build());
        }

        // Check if ports need to be exposed
        if (process.getExposePorts() != null) {
            for (final ExposePort exposePort : process.getExposePorts()) {
                endpointSpec.addPort(PortConfig.builder()
                        .publishedPort(exposePort.getExternal())
                        .targetPort(exposePort.getInternal())
                        .publishMode(PortConfigPublishMode.HOST)
                        .build());
            }
        }

        // If this is the dashboard gateway, open up the port that is configured in the environment var
        if (process.getServiceId().equals(ProcessManager.getDashboardGatewayServiceId())) {
            final int port = ProcessManager.getDashboardGatewayPort();
            endpointSpec.addPort(PortConfig.builder()
                    .publishedPort(port)
                    .targetPort(8080)
                    .publishMode(PortConfigPublishMode.HOST)
                    .build());
        }

        // Add resource limitations
        final Resources.Builder limits = Resources.builder();
        if (process.getMaxMemoryBytes() > 0) {
            limits.memoryBytes(process.getMaxMemoryBytes());
            jvmArgs += "-Xmx" + process.getMaxMemoryBytes();
        }
        if (process.getMaxNanoCPUs() > 0) {
            limits.nanoCpus(process.getMaxNanoCPUs());
        }
        final ResourceRequirements.Builder resources = ResourceRequirements.builder().limits(limits.build());

        // Add mounts to the container
        final List<Mount> mountList = new ArrayList<>();
        if (process.getMountPoints() != null) {
            for (final MountPoint mount : process.getMountPoints()) {
                mountList.add(Mount.builder().source(mount.getSource()).target(mount.getTarget()).build());
            }
            containerSpec.mounts(mountList);
        }

        final List<NetworkAttachmentConfig> networksConfigs = new ArrayList<>();
        for (final String networkName : networks) {
            networksConfigs.add(
                    NetworkAttachmentConfig.builder().target(networkName).aliases(process.getId().toString()).build());
        }

        // Set dEF-Pi parameters
        try {
            envArgs.put(DefPiParams.ORCHESTRATOR_HOST.name(), DockerConnector.getOrchestratorContainerId());
        } catch (final DockerException e) {
            ProcessConnector.log.error("Could not obtain hostame", e);
        }
        envArgs.put(DefPiParams.ORCHESTRATOR_PORT.name(), Integer.toString(Main.URI_PORT));
        envArgs.put(DefPiParams.ORCHESTRATOR_TOKEN.name(), process.getToken());
        envArgs.put(DefPiParams.PROCESS_ID.name(), process.getId().toString());
        envArgs.put(DefPiParams.USER_ID.name(), process.getUserId().toString());
        final User user = UserManager.getInstance().getUser(process.getUserId());
        envArgs.put(DefPiParams.USER_NAME.name(), user.getUsername());
        if (user.getEmail() != null) {
            envArgs.put(DefPiParams.USER_EMAIL.name(), user.getEmail());
        }

        // Add all container environment variables
        envArgs.put("JVM_ARGUMENTS", jvmArgs);
        final List<String> envList = new ArrayList<>();
        envArgs.forEach((key, value) -> envList.add(key + "=" + value));
        containerSpec.env(envList);

        // Set the logging configuration for the process
        taskSpec.logDriver(Driver.builder().name("json-file").addOption("max-size", "10m").build());

        // Add the containerSpec and placement to the taskSpec
        final Placement placement = Placement.create(Collections.singletonList("node.id == " + node.getDockerId()));
        taskSpec.containerSpec(containerSpec.build()).resources(resources.build()).placement(placement);
        return ServiceSpec.builder()
                .name(serviceName)
                .labels(serviceLabels)
                .taskTemplate(taskSpec.build())
                .endpointSpec(endpointSpec.build())
                .networks(networksConfigs)
                .build();
    }

    private static String getDockerServiceNameForProcess(final Process process) {
        return process.getId().toString();
    }

    private <T> T runOrTimeout(final Callable<T> callable) throws DockerException, InterruptedException {
        try {
            return this.executor.submit(callable)
                    .get(DockerConnector.DOCKER_WRITE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (final ExecutionException e) {
            if ((e.getCause() != null) && (e.getCause() instanceof DockerException)) {
                throw (DockerException) e.getCause();
            } else {
                throw new DockerException(e);
            }
        } catch (final TimeoutException e) {
            throw new DockerException(e);
        }
    }

    /**
     * Get some diagnostic information about the current running container. Very useful for validating the running
     * version of the orchestrator
     *
     * @return A string with some container info, or "UNKNOWN" if failed to obtain the information
     */
    public String getContainerInfo() {
        try {
            final ContainerInfo info = this.client.inspectContainer(DockerConnector.getOrchestratorContainerId());
            return String.format("image: %s\nbuilt: %s (by %s)\non branch %s\nlast commit: %s (%s)\nstarted: %s",
                    info.image(),
                    System.getenv(DockerConnector.BUILD_TIMESTAMP_KEY),
                    System.getenv(DockerConnector.BUILD_USER_KEY),
                    System.getenv(DockerConnector.GIT_BRANCH),
                    System.getenv(DockerConnector.GIT_COMMIT),
                    System.getenv(DockerConnector.GIT_LOG),
                    info.created().toInstant());
        } catch (DockerException | InterruptedException e) {
            DockerConnector.destroy("Error obtaining running image", e);
            return "Unknown";
        }
    }

    /**
     * Generate a health report as a simple String to show to the user. The health report should equal "Healthy" if
     * everything is according to the current configuration. Any incongruencies (services that have more or less running
     * tasks than desired) will show up in a separate line stating as X/Y, where X is the number of running tasks, and Y
     * is the number of desired instances.
     *
     * @return A String stating the running statuses of all current services (Not limited to dEF-Pi services).
     */
    public String getServiceHealth() {
        try {
            StringBuilder report = new StringBuilder();

            for (final com.spotify.docker.client.messages.swarm.Service s : this.client.listServices()) {
                final ServiceMode mode = s.spec().mode();
                if (mode == null) {
                    continue;
                }
                final ReplicatedService replicated = mode.replicated();
                if (replicated == null) {
                    continue;
                }
                final Long desiredAmount = replicated.replicas();
                if (desiredAmount == null) {
                    continue;
                }
                long runningAmount = 0;
                for (final Task t : this.client.listTasks(Criteria.builder().serviceName(s.id()).build())) {
                    runningAmount += t.status().state().equals("running") ? 1 : 0;
                }
                if (runningAmount != desiredAmount) {
                    report.append(String.format("\t%s: %d/%d\n", s.id(), runningAmount, desiredAmount));
                }
            }

            if (report.length() == 0) {
                return "Healthy";
            } else {
                return "*** Unhealthy! ***\n" + report.toString();
            }

        } catch (final DockerException | InterruptedException e) {
            DockerConnector.destroy("Error obtaining running image", e);
            return "Unknown";
        }
    }

}
