/**
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.bson.types.ObjectId;
import org.flexiblepower.exceptions.ApiException;
import org.flexiblepower.exceptions.ProcessNotFoundException;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.Architecture;
import org.flexiblepower.model.Node;
import org.flexiblepower.model.NodePool;
import org.flexiblepower.model.Process;
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
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.NetworkConfig;
import com.spotify.docker.client.messages.ServiceCreateResponse;
import com.spotify.docker.client.messages.mount.Mount;
import com.spotify.docker.client.messages.swarm.ContainerSpec;
import com.spotify.docker.client.messages.swarm.Driver;
import com.spotify.docker.client.messages.swarm.EndpointSpec;
import com.spotify.docker.client.messages.swarm.NetworkAttachmentConfig;
import com.spotify.docker.client.messages.swarm.Placement;
import com.spotify.docker.client.messages.swarm.PortConfig;
import com.spotify.docker.client.messages.swarm.PortConfig.PortConfigPublishMode;
import com.spotify.docker.client.messages.swarm.ResourceRequirements;
import com.spotify.docker.client.messages.swarm.Resources;
import com.spotify.docker.client.messages.swarm.ServiceSpec;
import com.spotify.docker.client.messages.swarm.Task;
import com.spotify.docker.client.messages.swarm.Task.Criteria;
import com.spotify.docker.client.messages.swarm.TaskSpec;

import lombok.extern.slf4j.Slf4j;

/**
 * DockerConnector
 *
 * @version 0.1
 * @since 20 mrt. 2017
 */
@Slf4j
public class DockerConnector {

    private static final int DOCKER_WRITE_TIMEOUT_MILLIS = 35000;
    private static final long DOCKER_CLIENT_TIMEOUT = 30000;

    private static final int INTERNAL_DEBUGGING_PORT = 8000;

    private static final String BUILD_TIMESTAMP_KEY = "BUILD_TIMESTAMP";
    private static final String BUILD_USER_KEY = "BUILD_USER";
    private static final String GIT_BRANCH = "GIT_BRANCH";
    private static final String GIT_COMMIT = "GIT_COMMIT";
    private static final String GIT_LOG = "GIT_LOG";
    private static final String DOCKER_HOST_KEY = "DOCKER_HOST";

    private static final String SERVICE_LABEL_KEY = "service.name";
    private static final String USER_LABEL_KEY = "user.id";
    private static final String NODE_ID_LABEL_KEY = "node.id";

    private static DockerConnector instance = null;

    private final Map<ObjectId, Object> netLocks = new ConcurrentHashMap<>();
    // private final Object createNetLock = new Object();
    private DockerClient client;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public static DockerClient init() throws DockerCertificateException {
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
    private synchronized void destroy(final String msg, final Throwable cause) {
        // this.client.close();
        DockerConnector.log.error("{}: {}", msg, cause.getMessage());
        DockerConnector.log.trace(cause.getMessage(), cause);
        // DockerConnector.instance = null;
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
    public String newProcess(final Process process) throws ServiceNotFoundException {
        try {
            if (!this.netLocks.containsKey(process.getUserId())) {
                this.netLocks.put(process.getUserId(), new Object());
            }
            // Synchronize by user to avoid many deadlocks
            synchronized (this.netLocks.get(process.getUserId())) {
                this.ensureProcessNetworkExists(process);
                this.ensureProcessNetworkIsAttached(process);
            }

            final Service service = ServiceManager.getInstance().getService(process.getServiceId());
            Node node = DockerConnector.determineRunningNode(process);

            ServiceSpec serviceSpec;
            if (process.getServiceId().equals(ProcessManager.getDashboardGatewayServiceId())) {
                // if this is the dashboard, it should be added to all user networks
                final List<String> networks = new ArrayList<>();

                final String dashboardNodeName = System.getenv(ProcessManager.DASHBOARD_GATEWAY_HOSTNAME_KEY);
                if (dashboardNodeName == null) {
                    DockerConnector.log.warn(
                            "No dashboard gateway host is specified, running on {}."
                                    + " To alter this behavior specify the system environment variable {}",
                            node.getHostname(),
                            ProcessManager.DASHBOARD_GATEWAY_HOSTNAME_KEY);
                } else {
                    final Node manuallySpecifiedNode = NodeManager.getInstance().getNodeByHostname(dashboardNodeName);
                    if (manuallySpecifiedNode == null) {
                        DockerConnector.log.warn(
                                "Could not find node with hostname %s, instead dashboard gateway will run on {}.",
                                dashboardNodeName,
                                node.getHostname());
                    }
                    node = manuallySpecifiedNode;
                }

                for (final User u : UserManager.getInstance().getUsers()) {
                    this.ensureUserNetworkExists(u);
                    networks.add(DockerConnector.getNetworkNameFromUser(u));
                }
                serviceSpec = DockerConnector.createServiceSpec(process, service, node, networks);
            } else {
                serviceSpec = DockerConnector.createServiceSpec(process, service, node);
            }
            final String id = this.executor.submit(() -> this.client.createService(serviceSpec).id())
                    .get(DockerConnector.DOCKER_WRITE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            DockerConnector.log.info("Created process with Id {}", id);

            return id;
        } catch (DockerException | InterruptedException | ExecutionException | TimeoutException e) {
            this.destroy("Exception while starting new process", e);
            return null;
        }
    }

    /**
     * @param uuid
     * @return
     * @throws ProcessNotFoundException
     * @throws ServiceNotFoundException
     */
    public boolean removeProcess(final Process process) throws ProcessNotFoundException {
        if (process.getDockerId() != null) {
            try {
                return this.executor.submit(() -> {
                    this.client.removeService(process.getDockerId());
                    return true;
                }).get(DockerConnector.DOCKER_WRITE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            } catch (final ExecutionException e) {
                if ((e.getCause() != null)
                        && (e.getCause() instanceof com.spotify.docker.client.exceptions.ServiceNotFoundException)) {
                    throw new ProcessNotFoundException(process.getId());
                } else {
                    this.destroy("Error while removing process", e);
                }
            } catch (final InterruptedException | TimeoutException e) {
                this.destroy("Error while removing process", e);
            }
        } else {
            // Container was probably never created
            return true;
        }
        return false;
    }

    /**
     * @return
     */
    public List<com.spotify.docker.client.messages.swarm.Node> listNodes() {
        try {
            return this.client.listNodes();
        } catch (DockerException | InterruptedException e) {
            this.destroy("Error while listing nodes", e);
            throw new ApiException(e);
        }
    }

    private void ensureProcessNetworkExists(final Process process) throws DockerException, InterruptedException {
        final String networkName = DockerConnector.getNetworkNameFromProcess(process);
        // check if it exists
        if (this.client.listNetworks(ListNetworksParam.byNetworkName(networkName)).isEmpty()) {
            // if (!this.listNetworks().values().contains(networkName)) {
            this.newNetwork(networkName);
        }
    }

    private void ensureUserNetworkExists(final User user) throws DockerException, InterruptedException {
        final String networkName = DockerConnector.getNetworkNameFromUser(user);
        // check if it exists
        if (this.client.listNetworks(ListNetworksParam.byNetworkName(networkName)).isEmpty()) {
            // if (!this.listNetworks().values().contains(networkName)) {
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
     * And connect to the new network
     *
     * @param networkName the name of the network to attach to
     * @throws InterruptedException
     * @throws DockerException
     */
    public void ensureProcessNetworkIsAttached(final Process process) throws InterruptedException, DockerException {
        try {
            final String newProcessNetworkName = DockerConnector.getNetworkNameFromProcess(process);
            final String networkId = this.client.listNetworks(ListNetworksParam.byNetworkName(newProcessNetworkName))
                    .get(0)
                    .id();

            // Connect orchestrator to network
            final String orchestratorContainerId = DockerConnector.getOrchestratorContainerId();
            final ContainerInfo orchestratorInfo = this.client.inspectContainer(orchestratorContainerId);
            if (!orchestratorInfo.networkSettings().networks().containsKey(newProcessNetworkName)) {
                DockerConnector.log.info("Connecting {} to network {}", orchestratorContainerId, newProcessNetworkName);
                try {
                    this.executor.submit(() -> {
                        this.client.connectToNetwork(orchestratorContainerId, newProcessNetworkName);
                        return true;
                    }).get(DockerConnector.DOCKER_WRITE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                } catch (ExecutionException | TimeoutException e) {
                    throw new DockerException("Execption waiting for docker client to connect", e);
                }
            }
            // Connect dashboard gateway to network
            final Process dashboardGateway = ProcessManager.getInstance().getDashboardGateway();
            if (dashboardGateway != null) {
                final String dockerServiceId = dashboardGateway.getDockerId();
                if (!dashboardGateway.getId().equals(process.getId()) && (dockerServiceId != null)) {
                    final com.spotify.docker.client.messages.swarm.Service dashboardInfo = this.client
                            .inspectService(dockerServiceId);
                    for (final NetworkAttachmentConfig network : dashboardInfo.spec().networks()) {
                        if (network.target().equals(networkId)) {
                            // It is already attached, we're done here!
                            return;
                        }
                    }
                    // If we're here that means that the dashboard proxy is not yet part of this network
                    this.updateDashboardGatewayService(process, dashboardGateway);
                }
            }
        } catch (DockerException | InterruptedException e) {
            this.destroy("Exception attaching process", e);
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
        try {
            this.executor.submit(() -> {
                this.client.removeService(dashboardGateway.getDockerId());
                return true;
            }).get(DockerConnector.DOCKER_WRITE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

            final ServiceCreateResponse newId = this.executor
                    .submit(() -> this.client.createService(DockerConnector.createServiceSpec(dashboardGateway,
                            ServiceManager.getInstance().getService(dashboardGateway.getServiceId()),
                            DockerConnector.determineRunningNode(process),
                            networks)))
                    .get(DockerConnector.DOCKER_WRITE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            dashboardGateway.setDockerId(newId.id());
            MongoDbConnector.getInstance().save(dashboardGateway);
        } catch (final ExecutionException | TimeoutException e) {
            this.destroy("Unable to update dashboard gateway", e);
            throw new DockerException(e);
        }
    }

    /**
     * Get the container id of the current container by getting the local hostname.
     *
     * @return
     * @throws DockerException
     */
    private static String getOrchestratorContainerId() throws DockerException {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (final UnknownHostException e) {
            throw new DockerException("Unable to get local container id by hostname", e);
        }
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
        try {
            return this.executor.submit(() -> this.client.createNetwork(networkConfig).id())
                    .get(DockerConnector.DOCKER_WRITE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (ExecutionException | TimeoutException e) {
            throw new DockerException(e);
        }
    }

    /**
     * @param networkId
     * @throws InterruptedException
     * @throws DockerException
     */
    @SuppressWarnings("unused")
    private void removeNetwork(final String networkId) throws DockerException, InterruptedException {
        this.client.disconnectFromNetwork(DockerConnector.getOrchestratorContainerId(), networkId);
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
        return DockerConnector.createServiceSpec(process,
                service,
                node,
                Collections.singletonList(DockerConnector.getNetworkNameFromProcess(process)));
    }

    /**
     * Internal function to translate a def-pi process definition to a docker service specification
     *
     * @param process
     * @return
     */
    private static ServiceSpec createServiceSpec(final Process process,
            final Service service,
            final Node node,
            final List<String> networks) {

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

        // If this is the dashboard gateway, open up the port that is configured in the environment var
        if (process.getServiceId().equals(ProcessManager.getDashboardGatewayServiceId())) {
            int port = ProcessManager.DASHBOARD_GATEWAY_PORT_DFLT;
            final String portFromEnv = System.getenv(ProcessManager.DASHBOARD_GATEWAY_PORT_KEY);
            if (portFromEnv != null) {
                try {
                    port = Integer.parseInt(portFromEnv);
                } catch (final NumberFormatException e) {
                    // We keep it at the default
                }
            }
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
        envArgs.put(DefPiParams.ORCHESTRATOR_TOKEN.name(),
                UserManager.getInstance().getUser(process.getUserId()).getAuthenticationToken());
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
        final Placement placement = Placement.create(Arrays.asList("node.id == " + node.getDockerId()));
        taskSpec.containerSpec(containerSpec.build()).resources(resources.build()).placement(placement);
        return ServiceSpec.builder()
                .name(serviceName)
                .labels(serviceLabels)
                .taskTemplate(taskSpec.build())
                .endpointSpec(endpointSpec.build())
                .networks(networksConfigs)
                .build();
    }

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
            this.destroy("Error obtaining running image", e);
            return "Unknown";
        }
    }

    public String getServiceHealth() {
        try {
            String report = "";

            for (final com.spotify.docker.client.messages.swarm.Service s : this.client.listServices()) {
                final long desiredAmount = s.spec().mode().replicated().replicas();
                long runningAmount = 0;
                for (final Task t : this.client.listTasks(Criteria.builder().serviceName(s.id()).build())) {
                    runningAmount += t.status().state().equals("running") ? 1 : 0;
                }
                if (runningAmount != desiredAmount) {
                    report += String.format("\t%s: %d/%d\n", s.id(), runningAmount, desiredAmount);
                }
            }

            if (report.isEmpty()) {
                return "Healthy";
            } else {
                return "*** Unhealthy! ***\n" + report;
            }

        } catch (final DockerException | InterruptedException e) {
            this.destroy("Error obtaining running image", e);
            return "Unknown";
        }
    }

}
