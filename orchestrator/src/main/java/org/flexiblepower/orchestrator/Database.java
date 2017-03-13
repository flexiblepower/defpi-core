package org.flexiblepower.orchestrator;

import static java.util.Arrays.asList;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.flexiblepower.gson.InitGson;
import org.flexiblepower.gson.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;

/**
 * @author Maarten Kollenstart
 * Database class that communicates with the MongoDB. 
 * The only method with a bit of logic is the linkAllowed method.
 */
public class Database {
//	private final static String host = "efpi-rd1.sensorlab.tno.nl";
	private final static String host = "192.168.137.15";
	
	private final static Logger logger = LoggerFactory.getLogger(Database.class);
	private final static MongoClient client = new MongoClient(host);
	private MongoDatabase db = client.getDatabase("db");
	private ObjectId user;
	private boolean admin = false;
	private Gson gson = InitGson.create();

	public Database() {
		LogManager.getLogger("org.mongodb.driver").setLevel(org.apache.log4j.Level.WARN);
	}
	
	public void setAdmin(boolean admin){
		this.admin = admin;
	}
	
	public void setUser(ObjectId user){
		this.user = user;
	}
	
	public ObjectId getUser(){
		return user;
	}
	
	public void updateUser(Document update){
		MongoCollection<Document> c = db.getCollection("users");
		c.updateOne(new Document("_id", user), update);
	}
	
	private Document addUserQuery(Document d){
		if(admin){
			return d;
		}
		return d.append("users", new Document("$eq", user));
	}

	public List<Document> getContainers() {
		logger.info("DB getContainers");
		final MongoCollection<Document> c = db.getCollection("containers");
		FindIterable<Document> iterable = c.find(addUserQuery(new Document()));
		final List<Document> containers = new ArrayList<Document>();
		iterable.forEach(new Block<Document>() {
			@Override
			public void apply(Document arg0) {
				arg0.append("links",
						db.getCollection("links")
								.find(new Document("$or",
										asList(new Document("container1", arg0.getString("uuid")),
												new Document("container2", arg0.getString("uuid")))))
								.into(new ArrayList<Document>()));
				if (!arg0.containsKey("ip")) {
					logger.info("Getting container IP");
					Containers c = new Containers(user);
					c.getContainerIP(arg0);
				}
				containers.add(arg0);
			}
		});
		return containers;
	}

	public Document getContainer(String uuid) {
		MongoCollection<Document> c = db.getCollection("containers");
		Document query = addUserQuery(new Document("uuid", uuid));
		logger.info("getContainer: "+query.toJson());
		Document r = c.find(query).first();
		return r;
	}

	public void updateContainer(Document filter, Document set) {
		MongoCollection<Document> c = db.getCollection("containers");
		logger.info("filter: " + filter);
		logger.info("set: " + set);
		c.updateOne(filter, set);
	}

	public List<Document> getServiceContainers(String image) {
		MongoCollection<Document> c = db.getCollection("containers");
		return c.find(new Document("image", addUserQuery(new Document("$regex", image + "$")))).into(new ArrayList<Document>());
	}

	public void insertContainer(String uuid, String name, String description, String container, String image, String tag,
			String host, Document interfaces, Map<String, String> environment) {
		MongoCollection<Document> c = db.getCollection("containers");
		c.insertOne(new Document("uuid", uuid)
				.append("name", name)
				.append("description", description)
				.append("container", container)
				.append("image", image)
				.append("tag", tag)
				.append("host", host)
				.append("interfaces", interfaces.get("interfaces"))
				.append("environment", environment)
				.append("users", Arrays.asList(user)));
	}

	public List<Document> findAutoconnectContainers(Object users, String subscribeHash, String publishHash) {
		MongoCollection<Document> c = db.getCollection("containers");
		FindIterable<Document> iterable = c
				.find(new Document("interfaces", new Document("$elemMatch", new Document("autoConnect", true)
						.append("publishHash", subscribeHash).append("subscribeHash", publishHash))));
		return iterable.into(new ArrayList<Document>());
	}

	public void deleteContainer(String uuid) {
		db.getCollection("containers").deleteOne(addUserQuery(new Document("uuid", uuid)));
	}

	public List<Document> getLinks() {
		MongoCollection<Document> c = db.getCollection("links");
		return c.find(addUserQuery(new Document())).into(new ArrayList<Document>());
	}

	public Document getLink(String id) {
		MongoCollection<Document> c = db.getCollection("links");
		return c.find(addUserQuery(new Document("id", id))).first();
	}

	public List<Document> getLinks(String uuid) {
		MongoCollection<Document> c = db.getCollection("links");
		FindIterable<Document> iterable = c.find(
				addUserQuery(new Document("$or", Arrays.asList(new Document("container1", uuid), new Document("container2", uuid)))));
		return iterable.into(new ArrayList<Document>());
	}

	@SuppressWarnings("unchecked")
	public void insertLink(Link link) {
		MongoCollection<Document> c = db.getCollection("links");
		Document container1 = getContainer(link.getContainer1());
		Document container2 = getContainer(link.getContainer2());
		List<ObjectId> users = container1.get("users", new ArrayList<ObjectId>().getClass());
		List<ObjectId> users2 = container2.get("users", new ArrayList<ObjectId>().getClass());
		users.addAll(users2);
		c.insertOne(Document.parse(gson.toJson(link)).append("users", users));
	}

	@SuppressWarnings("unchecked")
	public boolean linkAllowed(Link link) {
		MongoCollection<Document> c = db.getCollection("links");
		Document c1 = getContainer(link.getContainer1());
		Document c2 = getContainer(link.getContainer2());
		if (c1 == null || c2 == null)
			return false;
		List<Document> container1Interfaces = (List<Document>) c1.get("interfaces");
		List<Document> container2Interfaces = (List<Document>) c2.get("interfaces");
		for (Document i : container1Interfaces) {
			if(checkInterface(c, link, i, link.getContainer1())){
				return false;
			}
		}
		for (Document i : container2Interfaces) {
			if(checkInterface(c, link, i, link.getContainer2())){
				return false;
			}
		}

		return true;
	}
	
	private boolean checkInterface(MongoCollection<Document> c, Link link, Document i, String container){
		String subscribeHash = i.getString("subscribeHash");
		String publishHash = i.getString("publishHash");
		if (i.getInteger("cardinality") == 1) {
			if (subscribeHash.equals(link.getInterface1()) && publishHash.equals(link.getInterface2())
			||  subscribeHash.equals(link.getInterface2()) && publishHash.equals(link.getInterface1())) {
				
				Document search = new Document("$and",
						Arrays.asList(
								new Document("$or",
										Arrays.asList(new Document("container1", container),
												new Document("container2", container))),
								new Document("$or",
										Arrays.asList(
												new Document("interface1", link.getInterface1()).append("interface2", link.getInterface2()),
												new Document("interface1", link.getInterface2()).append("interface2", link.getInterface1())))));
				if (c.find(search).first() != null) {
					logger.info("  interface '"+i.getString("name") + "' is already connected");
					return true;
				}
			}
		}
		return false;
	}

	public void deleteLink(String id) {
		db.getCollection("links").deleteOne(new Document("id", id));
		
	}

	public void deleteLinks(String uuid) {
		db.getCollection("links").deleteOne(
				new Document("$or", Arrays.asList(new Document("container1", uuid), new Document("container2", uuid))));
	}

	public List<Document> getServices() {
		MongoCollection<Document> c = db.getCollection("services");
		return c.find().into(new ArrayList<Document>());
	}
	
	public Document getServices(String image){
		image = image.replace(Registry.registryLink + Registry.registryPrefix, "");
		MongoCollection<Document> c = db.getCollection("services");
		return c.find(new Document("image", image)).first();
	}
	
	public Document getService(String image, String tag){
		logger.info("Get service: "+image +" : "+tag);
		image = image.replace(Registry.registryLink + Registry.registryPrefix, "");
		MongoCollection<Document> c = db.getCollection("services");
		Document repository = c.find(new Document("image", image)).first();
		if (repository == null){
			return null;
		}
		
		@SuppressWarnings("unchecked")
		List<Document> tags = (List<Document>) repository.get("tags");
		for(Document d : tags){
			logger.info("Tag: "+ d);
			if(d.getString("tag").equals(tag)){
				return d;
			}
		}
		return null;
	}

	public List<Document> getProtos() {
		MongoCollection<Document> c = db.getCollection("protos");
		return c.find().into(new ArrayList<Document>());
	}

	public Document getProto(String sha256) {
		MongoCollection<Document> c = db.getCollection("protos");
		return c.find(new Document("sha256", sha256)).first();
	}

	public void insertProto(String name, String sha256, String proto) {
		MongoCollection<Document> c = db.getCollection("protos");
		c.insertOne(new Document("name", name).append("sha256", sha256).append("proto", proto));
	}

	public void deleteProto(String sha256) {
		MongoCollection<Document> c = db.getCollection("protos");
		c.deleteOne(new Document("sha256", sha256));
	}

	public Document getUser(String username, String password) {
		MongoCollection<Document> c = db.getCollection("users");
		return c.find(new Document("username", username).append("password", sha256(password))).first();
	}

	public void insertUser(String username, String password){
		MongoCollection<Document> c = db.getCollection("users");
		c.insertOne(new Document("username", username).append("password", sha256(password)));
	}
	public void deleteUser(String username){
		MongoCollection<Document> c = db.getCollection("users");
		c.deleteOne(new Document("username", username));
	}

	static String sha256(String input) {
		try {
			MessageDigest mDigest = MessageDigest.getInstance("SHA-256");
			byte[] result = mDigest.digest(input.getBytes());
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < result.length; i++) {
				sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
			}

			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			logger.error("No SHA256 algorithm");
		}
		return "";
	}
	
	public void upsertService(Document query, Document update) {
		MongoCollection<Document> c = db.getCollection("services");
		c.updateOne(query, new Document("$set", update), new UpdateOptions().upsert(true));
	}
	
	public List<Document> getHosts(){
		MongoCollection<Document> c = db.getCollection("hosts");
		return c.find(new Document("$or", Arrays.asList(new Document("labels.efpi-type", "public"), new Document("labels.efpi-type", "private").append("labels.efpi-user", user.toHexString())))).into(new ArrayList<Document>());
	}
	
	public List<Document> getHosts(String platform){
		MongoCollection<Document> c = db.getCollection("hosts");
		return c.find(
				new Document("$and",Arrays.asList(
						new Document("$or", 
								Arrays.asList(new Document("labels.efpi-type", "public")
										, new Document("labels.efpi-type", "private")
												.append("labels.efpi-user", user.toHexString())
											)
						),
						new Document("labels.platform", platform),
						new Document("state", "Healthy")
				))
			).into(new ArrayList<Document>());
	}
	
	public Document getHost(String id){
		MongoCollection<Document> c = db.getCollection("hosts");
		return c.find(new Document("id", id)).first();
	}
	
	public void removeHosts(){
		MongoCollection<Document> c = db.getCollection("hosts");
		c.deleteMany(new Document());
	}
	
	public void upsertHost(Document query, Document update) {
		MongoCollection<Document> c = db.getCollection("hosts");
		c.updateOne(query, new Document("$set", update), new UpdateOptions().upsert(true));
	}
	
	public boolean verifyUserHost(String hostId){
		logger.info("Verify: "+hostId+"  -  "+user.toHexString());
		MongoCollection<Document> c = db.getCollection("hosts");
		Document query = new Document("id", hostId).append("$or", Arrays.asList(
				new Document("labels.efpi-type", "public"),
				new Document("labels.efpi-type", "private").append("labels.efpi-user", user.toHexString())
		));
		logger.info(query.toJson());
		return c.find(query).first() != null;
	}

	public void deleteImage(String image) {
		MongoCollection<Document> c = db.getCollection("services");
		c.deleteOne(new Document("image", image));
	}

	public void updateImage(String image, List<Document> remaining) {
		MongoCollection<Document> c = db.getCollection("services");
		c.updateOne(new Document("image", image), new Document("$set", new Document("tags", remaining)));
	}
}
