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
import org.flexiblepower.gson.Link;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

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
	 * Database instance
	 */
	private Database d;
	/**
	 * Database identifier for the current user
	 */
	private ObjectId user;

	public Containers(ObjectId user) {
		d = new Database();
		d.setUser(user);
		this.user = user;
	}

	public Containers() {
		d = new Database();
	}

	public void setAdmin(boolean admin) {
		d.setAdmin(admin);
	}

	/**
	 * @return List of currently running containers for the current user
	 */
	public List<Document> listContainers() {
		logger.info("Containers listContainers");
		return d.getContainers();
	}

	/**
	 * Get the information on a single container
	 * 
	 * @param uuid
	 *            UUID of the container
	 * @return Current information available of the container
	 */
	public Document getContainer(String uuid) {
		return d.getContainer(uuid);
	}

	/**
	 * Delete a specific container, by sending a DELETE request to the Rancher
	 * host
	 * 
	 * @param uuid
	 *            UUID of the container
	 * @return HTTP Status number (200 = OK, 404 = NOT FOUND)
	 */
	public Status deleteContainer(String uuid) {
		Document c = d.getContainer(uuid);
		if (c != null) {
			List<Document> links = d.getLinks(uuid);
			Links l = new Links(user);
			for (Document link : links) {
				logger.info("   deleting link: " + link.toJson());
				l.deleteLink(link.getString("id"));
			}
			// Delete container via Rancher
			logger.info("   deleting container in rancher");

			Swarm.container(c.getString("container")+"?v=1&force=1")
					.request(MediaType.APPLICATION_JSON).buildDelete().invoke();
			logger.info("   deleting references");
			d.deleteContainer(uuid);
			d.deleteLinks(uuid);
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
	 *            Database object of the container
	 */
	public void getContainerIP(Document container) {
		logger.info("Get container IP");
		Response response = Swarm.container(container.getString("container"))
				.request(MediaType.APPLICATION_JSON_TYPE).get();
		String result = response.readEntity(String.class);
		logger.info("Swarm result: "+result);
		JSONObject json = new JSONObject(result);
		ContainerInfo containerInfo = new ContainerInfo();
		containerInfo.setHostId(json.getJSONObject("Node").getString("ID"));
		containerInfo.setId(json.getString("Id"));
		containerInfo.setIp(json.getJSONObject("NetworkSettings").getJSONObject("Networks").getJSONObject("overlay").getString("IPAddress"));
		containerInfo.setState(json.getJSONObject("State").getString("Status"));
		
		logger.info("Containerinfo: "+containerInfo);
		
		
		//ContainerInfo containerInfo = InitGson.create().fromJson(result,
		//		ContainerInfo.class);
		if (containerInfo.getIp() == null) {
			logger.info("IP not yet available");
		} else {
			container.append("ip", containerInfo.getIp());
			d.updateContainer(
					new Document("uuid", container.getString("uuid")),
					new Document("$set", new Document("ip", container
							.getString("ip"))).append("$unset", new Document(
							"upgrade", "")));
			if (container.containsKey("upgrade")) {
				reconnect(container);
			} else {
				autoConnect(container);
			}
		}
	}

	/**
	 * Reconnect container with the containers its last version was connected
	 * with
	 * 
	 * @param container		Database object of the container
	 */
	private void reconnect(Document container) {
		logger.info("Reconnect container: " + container.getString("uuid"));
		Links l = new Links(user);

		List<?> links = (List<?>) container.get("upgrade");
		logger.info("  reconnecting " + links.size() + " links");
		Gson gson = InitGson.create();
		for (Object linkObj : links) {
			Link link = gson
					.fromJson(((Document) linkObj).toJson(), Link.class);
			l.newLink(link);
		}
	}

	/**
	 * Automatically connect with containers that have matching interfaces
	 * that also want to auto connect.
	 * 
	 * @param container		Database object of the container
	 */
	public void autoConnect(Document container) {
		logger.info("Autoconnecting container: " + container.getString("uuid"));
		Links l = new Links(user);
		for (Object interfaceDoc : (List<?>) container.get("interfaces")) {
			Document i = (Document) interfaceDoc;
			if (i.getBoolean("autoConnect", false)) {
				for (Document match : d.findAutoconnectContainers(
						container.get("users"), i.getString("subscribeHash"),
						i.getString("publishHash"))) {
					if (match.containsKey("ip")) {
						logger.info("  connecting to : " + match.getString("uuid"));
						Link link = new Link(container.getString("uuid"),
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
	 * @param containerDescription	The parsed JSON indicating all options for 
	 * the new container
	 * @return	The UUID of the new container
	 */
	public String createContainer(ContainerDescription containerDescription) {
		UUID uuid = UUID.randomUUID();
		containerDescription.setUuid(uuid.toString());
		containerDescription.getEnvironment().put("SERVICE_UUID", uuid.toString());
		logger.info("Starting container, with environment: "
				+ containerDescription.getEnvironment());
		ContainerInfo containerInfo = Swarm.startContainer(
				containerDescription, d);
		if (containerInfo == null)
			return null;
		logger.info("  getting interfaces");
		Document interfaces = Registry
				.getInterfaceDocument(containerDescription.getImage(), containerDescription.getTag());
		logger.info("  creating reference");
		d.insertContainer(uuid.toString(), containerDescription.getName(),
				containerDescription.getDescription(), 
				Swarm.swarmHost	+ "containers/" + containerInfo.getId(),
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
	 * @param container		Database object of the container
	 * @return
	 */
	public Status upgradeContainer(Document container) {
		logger.info("Upgrade container: " + container.toJson());
		List<Document> links = d.getLinks(container.getString("uuid"));
		logger.info("  delete old container");
		Status deleteStatus = deleteContainer(container.getString("uuid"));
		if (deleteStatus != Status.OK) {
			return deleteStatus;
		}
		logger.info("  start new container");
		String uuid = createContainer(InitGson.create().fromJson(
				container.toJson(), ContainerDescription.class));
		logger.info("  change links references");
		for (Document link : links) {
			if (link.getString("container1")
					.equals(container.getString("uuid"))) {
				link.put("container1", uuid);
			} else {
				link.put("container2", uuid);
			}
		}
		logger.info("  update container");
		d.updateContainer(new Document("uuid", uuid), new Document("$set",
				new Document("upgrade", links)).append("$unset", new Document(
				"ip", "")));
		return Status.OK;
	}
}
