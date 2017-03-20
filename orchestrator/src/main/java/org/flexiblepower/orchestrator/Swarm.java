package org.flexiblepower.orchestrator;

import java.net.URI;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.flexiblepower.gson.ContainerDescription;
import org.flexiblepower.gson.ContainerInfo;
import org.flexiblepower.gson.swarm.SwarmContainerInfo;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerCertificates;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;

public class Swarm {

    private static final String CERT_PATH = "C:\\Users\\leeuwencjv\\.docker\\machine\\machines\\default";
    private static final String DOCKER_HOST = "https://192.168.137.111:2376/";

    private final static Random random = new Random();
    private final static Logger logger = LoggerFactory.getLogger(Swarm.class);

    private static List<String> omittedLabels = Arrays
            .asList("executiondriver", "kernelversion", "operatingsystem", "storagedriver");

    public static ContainerInfo startContainer(final ContainerDescription cd, final MongoDbConnector d)
            throws DockerCertificateException,
            DockerException,
            InterruptedException {
        final DockerClient docker = Swarm.init();

        /*
         * Gson gson = InitGson.create();
         * Client client = ClientBuilder.newClient();
         * WebTarget target = client.target(DOCKER_HOST+"containers/create");
         * Document host;
         *
         * if(cd.getImage().contains(":")){
         * String[] tmp = cd.getImage().split(":");
         * cd.setImage(tmp[0]);
         * cd.setTag(tmp[1]);
         * }
         *
         * if(!cd.getHost().equals("")){
         * if(!d.verifyUserHost(cd.getHost())){
         * return null;
         * }
         * host = d.getHost(cd.getHost());
         * }else{
         * List<Document> x86hosts = d.getHosts("x86");
         * if(!x86hosts.isEmpty()){
         * host = x86hosts.get(random.nextInt(x86hosts.size()));
         * }else{
         * List<Document> armhosts = d.getHosts("arm");
         * if(!armhosts.isEmpty()){
         * host = armhosts.get(random.nextInt(armhosts.size()));
         * }else{
         * logger.error("No suitable host find for: "+cd);
         * return null;
         * }
         * }
         * }
         * cd.getEnvironment().put("constraint:node=", host.getString("id"));
         * if(((Document) host.get("labels")).getString("platform").equals("arm")){
         * cd.setTag(cd.getTag()+"-arm");
         * }
         * WebTarget target2 =
         * client.target("http://"+host.getString("ip")+"/images/create?fromImage="+cd.getImage()+":"+cd.getTag());
         * target2.request().post(null);
         *
         * Document service = d.getService(cd.getImage(), cd.getTag());
         *
         * SwarmHostConfig swarmHostConfig = new SwarmHostConfig();
         * if(service != null && service.containsKey("mappings")){
         * logger.info("Found mappings");
         * List<String> mappings = (List<String>) service.get("mappings");
         * for(String mapping : mappings){
         * logger.info("Add mapping: "+mapping);
         * swarmHostConfig.getDevices().add(new SwarmDevices(mapping));
         * }
         * }
         * List<String> ports = null;
         * if(service != null && service.containsKey("ports")){
         * logger.info("Found exposed ports");
         * ports = (List<String>) service.get("ports");
         * Map<String, List<Map<String, String>>> bindings = new HashMap<String, List<Map<String, String>>>();
         * swarmHostConfig.setPortBindings(bindings);
         * for(String port : ports){
         * List<Map<String,String>> portList = new ArrayList<Map<String,String>>();
         * Map<String, String> binding = new HashMap<String,String>();
         * portList.add(binding);
         * binding.put("HostPort", port);
         * bindings.put(port+"/tcp", portList);
         * }
         * }
         * SwarmCreate swarmCreate = new SwarmCreate(cd.getName(), cd.getEnvironment(), cd.getImage(), cd.getTag(),
         * ports, swarmHostConfig);
         * logger.info("Request: "+gson.toJson(swarmCreate));
         * // TODO: Add public/private deployment
         * Response response = target.request(MediaType.APPLICATION_JSON_TYPE)
         * .post(Entity.entity(gson.toJson(swarmCreate), MediaType.APPLICATION_JSON_TYPE));
         * String swarmResponse = response.readEntity(String.class);
         * //SwarmCreateResponse swarmCreateResponse = gson.fromJson(swarmResponse, SwarmCreateResponse.class);
         * logger.info(swarmResponse);
         * SwarmContainerInfo swarmContainerInfo = Swarm.containerInfo(new JSONObject(swarmResponse).getString("Id"));
         * WebTarget target3 = client.target(DOCKER_HOST+"containers/"+swarmContainerInfo.getId()+"/start");
         * int startStatus = target3.request().post(null).getStatus();
         * logger.info("Container info: ("+startStatus+") "+swarmContainerInfo);
         */

        // final ContainerSpec containerSpec = ContainerSpec.builder().image(cd.getImage()).build();
        //
        // final Document host = d.getHost(cd.getHost());
        // final List<String> constraints = Arrays.asList(String.format("constraint:node=%s", host.getString("id")));
        // final Placement placement = Placement.create(constraints);
        // final TaskSpec taskTemplate = TaskSpec.builder().containerSpec(containerSpec).placement(placement).build();
        //
        // // EndpointSpec.builder().po
        //
        // final ServiceSpec serviceSpec = ServiceSpec.builder().taskTemplate(taskTemplate).build();
        //
        // final ServiceCreateResponse response = docker.createService(serviceSpec);
        //
        // final Service service = docker.inspectService(response.id());

        return null;

        // return new ContainerInfo(swarmContainerInfo.getId(), swarmContainerInfo.getState(),
        // swarmContainerInfo.getNode(), swarmContainerInfo.getIp());
    }

    public static SwarmContainerInfo containerInfo(final String id) {
        final Client client = ClientBuilder.newClient();
        final WebTarget target = client.target(Swarm.DOCKER_HOST + "containers/" + id + "/json");
        final String response = target.request(MediaType.APPLICATION_JSON_TYPE).get().readEntity(String.class);
        Swarm.logger.info("Info: " + Swarm.DOCKER_HOST + "containers/" + id + "/json" + " -- " + response);
        final JSONObject container = new JSONObject(response);
        final String ip = container.getJSONObject("NetworkSettings")
                .getJSONObject("Networks")
                .getJSONObject("overlay")
                .getString("IPAddress");
        final String node = container.getJSONObject("Node").getString("ID");
        final String state = container.getJSONObject("State").getString("Status");
        return new SwarmContainerInfo(id, ip, node, state);
    }

    public static void syncHosts() throws DockerCertificateException, DockerException, InterruptedException {
        // final DockerClient docker = Swarm.init();
        // final List<Node> nodelist = docker.listNodes();
        //
        // final Gson gson = InitGson.create();
        //
        // final MongoDbConnector db = new MongoDbConnector();
        // db.removeHosts();
        // for (final Node n : nodelist) {
        // final Host host = new Host(n.id(), n.spec().name(), n.description().hostname());
        // host.setLabels(n.spec().labels());
        // host.setState(n.spec().availability());
        // db.upsertHost(new Document("id", n.id()), Document.parse(gson.toJson(host)));
        // }

        // SwarmSpec specs = docker.inspectSwarm().swarmSpec();
        // specs.caConfig()
        // Network network = docker.inspectNetwork(networkId);

        /*
         * Gson gson = InitGson.create();
         * Client client = ClientBuilder.newClient();
         * WebTarget target = client.target(DOCKER_HOST+"info");
         * Response response = target.request(MediaType.APPLICATION_JSON_TYPE).get();
         * JSONObject result = new JSONObject(response.readEntity(String.class));
         * JSONArray systemstatus = result.getJSONArray("SystemStatus");
         * JSONArray previous = null;
         * HostList hostList = new HostList();
         * hostList.setData(new ArrayList<Host>());
         * Host last = null;
         * for (Iterator<?> iterator = systemstatus.iterator(); iterator.hasNext();) {
         * JSONArray entry = (JSONArray) iterator.next();
         * if(entry.getString(0).equals("  └ ID")){
         * last = new Host(entry.getString(1),previous.getString(0).trim(), previous.getString(1));
         * hostList.getData().add(last);
         * }else if(entry.getString(0).equals("  └ Status")){
         * last.setState(entry.getString(1));
         * }else if(entry.getString(0).equals("  └ Labels")){
         * for(String label : entry.getString(1).split(",")){
         * String[] labelEntry = label.trim().split("=");
         * if(labelEntry.length == 2 && !omittedLabels.contains(labelEntry[0])){
         * last.getLabels().put(labelEntry[0].replace("efpi.", ""), labelEntry[1]);
         * }
         * }
         * }
         * previous = entry;
         * }
         * MongoDbConnector d = new MongoDbConnector();
         * d.removeHosts();
         * for(Host host : hostList.getData()){
         * d.upsertHost(new Document("id", host.getId()), Document.parse(gson.toJson(host)));
         * }
         */

    }

    public static void containerSync() {
        final Client client = ClientBuilder.newClient();
        final WebTarget target = client.target(Swarm.DOCKER_HOST + "containers/json");
        final Response response = target.request(MediaType.APPLICATION_JSON_TYPE).get();
        final JSONArray result = new JSONObject(response.readEntity(String.class)).getJSONArray("data");
        final Containers containers = new Containers();
        containers.setAdmin(true);
        for (final Object name : result) {
            final JSONObject container = (JSONObject) name;
            if (container.getString("Image").startsWith(Registry.registryLink + Registry.registryPrefix + "/services/")
                    && container.getString("state").equals("exited")) {
                final String uuid = container.getJSONObject("Labels").getString("org.flexiblepower.SERVICE_UUID");
                containers.deleteContainer(uuid);
            }
        }
    }

    public static String newHost() {
        return "";
    }

    public static int hostAction(final String host, final String action) {
        return 404;
    }

    public static WebTarget container(final String link) {
        final Client client = ClientBuilder.newClient();
        return client.target(link + "/json");
    }

    public static DockerClient init() throws DockerCertificateException, DockerException, InterruptedException {
        return DefaultDockerClient.builder()
                .uri(URI.create(Swarm.DOCKER_HOST))
                .dockerCertificates(new DockerCertificates(Paths.get(Swarm.CERT_PATH)))
                .build();
    }

    public static void
            main(final String[] args) throws DockerCertificateException, DockerException, InterruptedException {
        final DockerClient docker = Swarm.init();

        System.out.println(docker.listImages());
        System.out.println(docker.listNodes());

    }

}
