package org.flexiblepower.orchestrator;

import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.flexiblepower.gson.ContainerDescription;
import org.flexiblepower.gson.ContainerInfo;
import org.flexiblepower.gson.InitGson;
import org.flexiblepower.model.Connection;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;

/**
 * @author Maarten Kollenstart
 *
 *         Orchestration for containers, with options to retrieve information of
 *         containers. And applying actions to containers.
 */
public class Containers {

    /**
     * SLF4J Logger
     */
    final static Logger logger = LoggerFactory.getLogger(Containers.class);
    /**
     * MongoDbConnector instance
     */
    private final MongoDbConnector d;
    /**
     * MongoDbConnector identifier for the current user
     */
    private ObjectId user;

    public Containers(final ObjectId user) {
        this.d = new MongoDbConnector();
        this.d.setApplicationUser(user);
        this.user = user;
    }

    public Containers() {
        this.d = new MongoDbConnector();
    }

    public void setAdmin(final boolean admin) {
        this.d.setAdmin(admin);
    }

    /**
     * @return List of currently running containers for the current user
     */
    public List<Document> listContainers() {
        Containers.logger.info("Containers listContainers");
        return this.d.getContainers();
    }

    /**
     * Get the information on a single container
     *
     * @param uuid
     *            UUID of the container
     * @return Current information available of the container
     */
    public Document getContainer(final String uuid) {
        return this.d.getContainer(uuid);
    }

    /**
     * Delete a specific container, by sending a DELETE request to the Rancher
     * host
     *
     * @param uuid
     *            UUID of the container
     * @return HTTP Status number (200 = OK, 404 = NOT FOUND)
     */
    public Status deleteContainer(final String uuid) {
        final Document c = this.d.getContainer(uuid);
        if (c != null) {
            final List<Document> links = this.d.getLinks(uuid);
            final Links l = new Links(this.user);
            for (final Document link : links) {
                Containers.logger.info("   deleting link: " + link.toJson());
                l.deleteLink(link.getString("id"));
            }
            // Delete container via Rancher
            Containers.logger.info("   deleting container in rancher");

            Swarm.container(c.getString("container") + "?v=1&force=1")
                    .request(MediaType.APPLICATION_JSON)
                    .buildDelete()
                    .invoke();
            Containers.logger.info("   deleting references");
            this.d.deleteContainer(uuid);
            this.d.deleteLinks(uuid);
            return Status.OK;
        } else {
            return Status.NOT_FOUND;
        }
    }

    /**
     * Check if the container has received an IP address yet, indicating the
     * container has started. In case it has an IP address the orchestrator will
     * try to auto connect this container to other containers.
     *
     * @param container
     *            MongoDbConnector object of the container
     */
    public void getContainerIP(final Document container) {
        Containers.logger.info("Get container IP");
        final Response response = Swarm.container(container.getString("container"))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();
        final String result = response.readEntity(String.class);
        Containers.logger.info("Swarm result: " + result);
        final JSONObject json = new JSONObject(result);
        final ContainerInfo containerInfo = new ContainerInfo();
        containerInfo.setHostId(json.getJSONObject("Node").getString("ID"));
        containerInfo.setId(json.getString("Id"));
        containerInfo.setIp(
                json.getJSONObject("NetworkSettings").getJSONObject("Networks").getJSONObject("overlay").getString(
                        "IPAddress"));
        containerInfo.setState(json.getJSONObject("State").getString("Status"));

        Containers.logger.info("Containerinfo: " + containerInfo);

        // ContainerInfo containerInfo = InitGson.create().fromJson(result,
        // ContainerInfo.class);
        if (containerInfo.getIp() == null) {
            Containers.logger.info("IP not yet available");
        } else {
            container.append("ip", containerInfo.getIp());
            this.d.updateContainer(new Document("uuid", container.getString("uuid")),
                    new Document("$set", new Document("ip", container.getString("ip"))).append("$unset",
                            new Document("upgrade", "")));
            if (container.containsKey("upgrade")) {
                this.reconnect(container);
            } else {
                this.autoConnect(container);
            }
        }
    }

    /**
     * Reconnect container with the containers its last version was connected
     * with
     *
     * @param container MongoDbConnector object of the container
     */
    private void reconnect(final Document container) {
        Containers.logger.info("Reconnect container: " + container.getString("uuid"));
        final Links l = new Links(this.user);

        final List<?> links = (List<?>) container.get("upgrade");
        Containers.logger.info("  reconnecting " + links.size() + " links");
        final Gson gson = InitGson.create();
        for (final Object linkObj : links) {
            final Connection link = gson.fromJson(((Document) linkObj).toJson(), Connection.class);
            l.newLink(link);
        }
    }

    /**
     * Automatically connect with containers that have matching interfaces
     * that also want to auto connect.
     *
     * @param container MongoDbConnector object of the container
     */
    public void autoConnect(final Document container) {
        Containers.logger.info("Autoconnecting container: " + container.getString("uuid"));
        final Links l = new Links(this.user);
        for (final Object interfaceDoc : (List<?>) container.get("interfaces")) {
            final Document i = (Document) interfaceDoc;
            if (i.getBoolean("autoConnect", false)) {
                for (final Document match : this.d.findAutoconnectContainers(container.get("users"),
                        i.getString("subscribeHash"),
                        i.getString("publishHash"))) {
                    if (match.containsKey("ip")) {
                        Containers.logger.info("  connecting to : " + match.getString("uuid"));
                        final Connection link = new Connection(container.getString("uuid"),
                                match.getString("uuid"),
                                i.getString("subscribeHash"),
                                i.getString("publishHash"));
                        l.newLink(link);
                    }
                }
            }
        }
    }

    /**
     * Create a new container based on the data transfer object
     * ContainerDescription
     *
     * @param containerDescription The parsed JSON indicating all options for
     *            the new container
     * @return The UUID of the new container
     * @throws InterruptedException
     * @throws DockerException
     * @throws DockerCertificateException
     */
    public String createContainer(final ContainerDescription containerDescription) throws DockerCertificateException,
            DockerException,
            InterruptedException {
        final UUID uuid = UUID.randomUUID();
        containerDescription.setUuid(uuid.toString());
        containerDescription.getEnvironment().put("SERVICE_UUID", uuid.toString());
        Containers.logger.info("Starting container, with environment: " + containerDescription.getEnvironment());
        final ContainerInfo containerInfo = Swarm.startContainer(containerDescription, this.d);
        if (containerInfo == null) {
            return null;
        }
        Containers.logger.info("  getting interfaces");
        final Document interfaces = Registry.getInterfaceDocument(containerDescription.getImage(),
                containerDescription.getTag());
        Containers.logger.info("  creating reference");
        this.d.insertContainer(uuid.toString(),
                containerDescription.getName(),
                containerDescription.getDescription(),
                "<SWARM_HOST>" + "containers/" + containerInfo.getId(),
                containerDescription.getImage(),
                containerDescription.getTag(),
                containerInfo.getHostId(),
                interfaces,
                containerDescription.getEnvironment());
        return uuid.toString();
    }

    /**
     * Upgrade a container, that is, stopping the old container and start the new
     * container while forcing to download the service image again.
     *
     * @param container MongoDbConnector object of the container
     * @return
     * @throws InterruptedException
     * @throws DockerException
     * @throws DockerCertificateException
     * @throws JsonSyntaxException
     */
    public Status upgradeContainer(final Document container) throws JsonSyntaxException,
            DockerCertificateException,
            DockerException,
            InterruptedException {
        Containers.logger.info("Upgrade container: " + container.toJson());
        final List<Document> links = this.d.getLinks(container.getString("uuid"));
        Containers.logger.info("  delete old container");
        final Status deleteStatus = this.deleteContainer(container.getString("uuid"));
        if (deleteStatus != Status.OK) {
            return deleteStatus;
        }
        Containers.logger.info("  start new container");
        final String uuid = this
                .createContainer(InitGson.create().fromJson(container.toJson(), ContainerDescription.class));
        Containers.logger.info("  change links references");
        for (final Document link : links) {
            if (link.getString("container1").equals(container.getString("uuid"))) {
                link.put("container1", uuid);
            } else {
                link.put("container2", uuid);
            }
        }
        Containers.logger.info("  update container");
        this.d.updateContainer(new Document("uuid", uuid),
                new Document("$set", new Document("upgrade", links)).append("$unset", new Document("ip", "")));
        return Status.OK;
    }
}
