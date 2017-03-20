package org.flexiblepower.orchestrator;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.flexiblepower.gson.InitGson;
import org.flexiblepower.gson.RegistryCatalog;
import org.flexiblepower.gson.RegistryTags;
import org.flexiblepower.gson.Repository;
import org.flexiblepower.gson.Service;
import org.flexiblepower.gson.ServiceLabels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Services {
	final static Logger logger = LoggerFactory.getLogger(Services.class);
	private MongoDbConnector d;

	public Services(ObjectId user) {
		d = new MongoDbConnector();
		d.setApplicationUser(user);
	}

	public Services() {
		d = new MongoDbConnector();
	}

	public List<Document> listServices() {
		return d.getServices();
	}

	public Document listService(String image, String tag) {
		return d.getService(image, tag);
	}

	public void syncServices() {
		RegistryCatalog registryCatalog = Registry.getRepositories();
		for (String serviceImage : registryCatalog.getRepositories()) {
			List<Service> tags = new ArrayList<Service>();
			if (serviceImage.startsWith(Registry.registryPrefix+"services/")) {
				serviceImage = serviceImage.replace(Registry.registryPrefix, "");
				RegistryTags registryTags = Registry.getTags(serviceImage);
				if (registryTags.getTags() != null) {
					String name = "";
					for (String tag : registryTags.getTags()) {
						ServiceLabels serviceLabels = Registry.getLabels(serviceImage, tag);
						Service service = new Service(serviceLabels.getName(), serviceLabels.getInterfaces(),
								serviceLabels.getMappings(), serviceLabels.getPorts(), serviceImage, tag, serviceLabels.getCreated());
						name = serviceLabels.getName();
						tags.add(service);
					}
					Repository r = new Repository(name, serviceImage, tags);
					d.upsertService(new Document("image", serviceImage), Document.parse(InitGson.create().toJson(r)));
				}
			}
		}
	}

	public void deleteService(String image, String tagString) {
		try {
			Document repo = d.getServices(image);
			@SuppressWarnings("unchecked")
			List<Document> tags = (List<Document>) repo.get("tags");
			List<Document> remaining = new ArrayList<Document>();
			for (Document tag : tags) {
				if (!tag.getString("tag").equals(tagString)) {
					remaining.add(tag);
				}
			}
			if (remaining.size() == 0) {
				logger.info("No tags remaining for image, deleting image reference");
				d.deleteImage(image);
			} else {
				logger.info(remaining.size() + " tags remaining, keeping image reference");
				d.updateImage(image, remaining);
			}
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			sw.toString();
			logger.error(e.toString() + "\n\n" + sw);
		}
		Registry.deleteTag(image, tagString);
	}
}
