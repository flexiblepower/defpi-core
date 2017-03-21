package org.flexiblepower.orchestrator;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.flexiblepower.gson.InitGson;
import org.flexiblepower.gson.Link;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
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
 *         Database class that communicates with the MongoDB.
 *         The only method with a bit of logic is the linkAllowed method.
 */
public class Database {

    // private final static String host = "efpi-rd1.sensorlab.tno.nl";
    private final static String host = "192.168.137.177";

    private final static Logger logger = LoggerFactory.getLogger(Database.class);
    private final static MongoClient client = new MongoClient(Database.host);
    private final MongoDatabase db = Database.client.getDatabase("db");
    private ObjectId user;
    private boolean admin = false;
    private final Gson gson = InitGson.create();

    public Database() {
        final Morphia morphia = new Morphia();

        // tell Morphia where to find your classes
        // can be called multiple times with different packages or classes
        morphia.mapPackage("org.flexiblepower");

        // create the Datastore connecting to the default port on the local host
        final Datastore datastore = morphia.createDatastore(Database.client, "db");
        datastore.ensureIndexes();
    }

    public void setAdmin(final boolean admin) {
        this.admin = admin;
    }

    public void setUser(final ObjectId user) {
        this.user = user;
    }

    public ObjectId getUser() {
        return this.user;
    }

    public void updateUser(final Document update) {
        final MongoCollection<Document> c = this.db.getCollection("users");
        c.updateOne(new Document("_id", this.user), update);
    }

    private Document addUserQuery(final Document d) {
        if (this.admin) {
            return d;
        }
        return d.append("users", new Document("$eq", this.user));
    }

    public List<Document> getContainers() {
        Database.logger.info("DB getContainers");
        final MongoCollection<Document> c = this.db.getCollection("containers");
        final FindIterable<Document> iterable = c.find(this.addUserQuery(new Document()));
        final List<Document> containers = new ArrayList<>();
        iterable.forEach(new Block<Document>() {

            @Override
            public void apply(final Document arg0) {
                arg0.append("links",
                        Database.this.db.getCollection("links")
                                .find(new Document("$or",
                                        Arrays.asList(new Document("container1", arg0.getString("uuid")),
                                                new Document("container2", arg0.getString("uuid")))))
                                .into(new ArrayList<Document>()));
                if (!arg0.containsKey("ip")) {
                    Database.logger.info("Getting container IP");
                    final Containers c = new Containers(Database.this.user);
                    c.getContainerIP(arg0);
                }
                containers.add(arg0);
            }
        });
        return containers;
    }

    public Document getContainer(final String uuid) {
        final MongoCollection<Document> c = this.db.getCollection("containers");
        final Document query = this.addUserQuery(new Document("uuid", uuid));
        Database.logger.info("getContainer: " + query.toJson());
        final Document r = c.find(query).first();
        return r;
    }

    public void updateContainer(final Document filter, final Document set) {
        final MongoCollection<Document> c = this.db.getCollection("containers");
        Database.logger.info("filter: " + filter);
        Database.logger.info("set: " + set);
        c.updateOne(filter, set);
    }

    public List<Document> getServiceContainers(final String image) {
        final MongoCollection<Document> c = this.db.getCollection("containers");
        return c.find(new Document("image", this.addUserQuery(new Document("$regex", image + "$"))))
                .into(new ArrayList<Document>());
    }

    public void insertContainer(final String uuid,
            final String name,
            final String description,
            final String container,
            final String image,
            final String tag,
            final String host,
            final Document interfaces,
            final Map<String, String> environment) {
        final MongoCollection<Document> c = this.db.getCollection("containers");
        c.insertOne(new Document("uuid", uuid).append("name", name)
                .append("description", description)
                .append("container", container)
                .append("image", image)
                .append("tag", tag)
                .append("host", host)
                .append("interfaces", interfaces.get("interfaces"))
                .append("environment", environment)
                .append("users", Arrays.asList(this.user)));
    }

    public List<Document>
            findAutoconnectContainers(final Object users, final String subscribeHash, final String publishHash) {
        final MongoCollection<Document> c = this.db.getCollection("containers");
        final FindIterable<Document> iterable = c.find(new Document("interfaces",
                new Document("$elemMatch",
                        new Document("autoConnect", true).append("publishHash", subscribeHash).append("subscribeHash",
                                publishHash))));
        return iterable.into(new ArrayList<Document>());
    }

    public void deleteContainer(final String uuid) {
        this.db.getCollection("containers").deleteOne(this.addUserQuery(new Document("uuid", uuid)));
    }

    public List<Document> getLinks() {
        final MongoCollection<Document> c = this.db.getCollection("links");
        return c.find(this.addUserQuery(new Document())).into(new ArrayList<Document>());
    }

    public Document getLink(final String id) {
        final MongoCollection<Document> c = this.db.getCollection("links");
        return c.find(this.addUserQuery(new Document("id", id))).first();
    }

    public List<Document> getLinks(final String uuid) {
        final MongoCollection<Document> c = this.db.getCollection("links");
        final FindIterable<Document> iterable = c.find(this.addUserQuery(new Document("$or",
                Arrays.asList(new Document("container1", uuid), new Document("container2", uuid)))));
        return iterable.into(new ArrayList<Document>());
    }

    @SuppressWarnings("unchecked")
    public void insertLink(final Link link) {
        final MongoCollection<Document> c = this.db.getCollection("links");
        final Document container1 = this.getContainer(link.getContainer1());
        final Document container2 = this.getContainer(link.getContainer2());
        final List<ObjectId> users = container1.get("users", new ArrayList<ObjectId>().getClass());
        final List<ObjectId> users2 = container2.get("users", new ArrayList<ObjectId>().getClass());
        users.addAll(users2);
        c.insertOne(Document.parse(this.gson.toJson(link)).append("users", users));
    }

    @SuppressWarnings("unchecked")
    public boolean linkAllowed(final Link link) {
        final MongoCollection<Document> c = this.db.getCollection("links");
        final Document c1 = this.getContainer(link.getContainer1());
        final Document c2 = this.getContainer(link.getContainer2());
        if ((c1 == null) || (c2 == null)) {
            return false;
        }
        final List<Document> container1Interfaces = (List<Document>) c1.get("interfaces");
        final List<Document> container2Interfaces = (List<Document>) c2.get("interfaces");
        for (final Document i : container1Interfaces) {
            if (this.checkInterface(c, link, i, link.getContainer1())) {
                return false;
            }
        }
        for (final Document i : container2Interfaces) {
            if (this.checkInterface(c, link, i, link.getContainer2())) {
                return false;
            }
        }

        return true;
    }

    private boolean checkInterface(final MongoCollection<Document> c,
            final Link link,
            final Document i,
            final String container) {
        final String subscribeHash = i.getString("subscribeHash");
        final String publishHash = i.getString("publishHash");
        if (i.getInteger("cardinality") == 1) {
            if ((subscribeHash.equals(link.getInterface1()) && publishHash.equals(link.getInterface2()))
                    || (subscribeHash.equals(link.getInterface2()) && publishHash.equals(link.getInterface1()))) {

                final Document search = new Document("$and",
                        Arrays.asList(
                                new Document("$or",
                                        Arrays.asList(new Document("container1", container),
                                                new Document("container2", container))),
                                new Document("$or",
                                        Arrays.asList(
                                                new Document("interface1", link.getInterface1()).append("interface2",
                                                        link.getInterface2()),
                                                new Document("interface1", link.getInterface2()).append("interface2",
                                                        link.getInterface1())))));
                if (c.find(search).first() != null) {
                    Database.logger.info("  interface '" + i.getString("name") + "' is already connected");
                    return true;
                }
            }
        }
        return false;
    }

    public void deleteLink(final String id) {
        this.db.getCollection("links").deleteOne(new Document("id", id));

    }

    public void deleteLinks(final String uuid) {
        this.db.getCollection("links").deleteOne(
                new Document("$or", Arrays.asList(new Document("container1", uuid), new Document("container2", uuid))));
    }

    public List<Document> getServices() {
        final MongoCollection<Document> c = this.db.getCollection("services");
        return c.find().into(new ArrayList<Document>());
    }

    public Document getServices(String image) {
        image = image.replace(Registry.registryLink + Registry.registryPrefix, "");
        final MongoCollection<Document> c = this.db.getCollection("services");
        return c.find(new Document("image", image)).first();
    }

    public Document getService(String image, final String tag) {
        Database.logger.info("Get service: " + image + " : " + tag);
        image = image.replace(Registry.registryLink + Registry.registryPrefix, "");
        final MongoCollection<Document> c = this.db.getCollection("services");
        final Document repository = c.find(new Document("image", image)).first();
        if (repository == null) {
            return null;
        }

        @SuppressWarnings("unchecked")
        final List<Document> tags = (List<Document>) repository.get("tags");
        for (final Document d : tags) {
            Database.logger.info("Tag: " + d);
            if (d.getString("tag").equals(tag)) {
                return d;
            }
        }
        return null;
    }

    public List<Document> getProtos() {
        final MongoCollection<Document> c = this.db.getCollection("protos");
        return c.find().into(new ArrayList<Document>());
    }

    public Document getProto(final String sha256) {
        final MongoCollection<Document> c = this.db.getCollection("protos");
        return c.find(new Document("sha256", sha256)).first();
    }

    public void insertProto(final String name, final String sha256, final String proto) {
        final MongoCollection<Document> c = this.db.getCollection("protos");
        c.insertOne(new Document("name", name).append("sha256", sha256).append("proto", proto));
    }

    public void deleteProto(final String sha256) {
        final MongoCollection<Document> c = this.db.getCollection("protos");
        c.deleteOne(new Document("sha256", sha256));
    }

    public Document getUser(final String username, final String password) {
        final MongoCollection<Document> c = this.db.getCollection("users");
        return c.find(new Document("username", username).append("password", Database.sha256(password))).first();
    }

    public void insertUser(final String username, final String password) {
        final MongoCollection<Document> c = this.db.getCollection("users");
        c.insertOne(new Document("username", username).append("password", Database.sha256(password)));
    }

    public void deleteUser(final String username) {
        final MongoCollection<Document> c = this.db.getCollection("users");
        c.deleteOne(new Document("username", username));
    }

    static String sha256(final String input) {
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("SHA-256");
            final byte[] result = mDigest.digest(input.getBytes());
            final StringBuffer sb = new StringBuffer();
            for (final byte element : result) {
                sb.append(Integer.toString((element & 0xff) + 0x100, 16).substring(1));
            }

            return sb.toString();
        } catch (final NoSuchAlgorithmException e) {
            Database.logger.error("No SHA256 algorithm");
        }
        return "";
    }

    public void upsertService(final Document query, final Document update) {
        final MongoCollection<Document> c = this.db.getCollection("services");
        c.updateOne(query, new Document("$set", update), new UpdateOptions().upsert(true));
    }

    public List<Document> getHosts() {
        final MongoCollection<Document> c = this.db.getCollection("hosts");
        return c.find(new Document("$or",
                Arrays.asList(new Document("labels.efpi-type", "public"),
                        new Document("labels.efpi-type", "private").append("labels.efpi-user",
                                this.user.toHexString()))))
                .into(new ArrayList<Document>());
    }

    public List<Document> getHosts(final String platform) {
        final MongoCollection<Document> c = this.db.getCollection("hosts");
        return c.find(new Document("$and",
                Arrays.asList(
                        new Document("$or",
                                Arrays.asList(new Document("labels.efpi-type", "public"),
                                        new Document("labels.efpi-type", "private").append("labels.efpi-user",
                                                this.user.toHexString()))),
                        new Document("labels.platform", platform),
                        new Document("state", "Healthy"))))
                .into(new ArrayList<Document>());
    }

    public Document getHost(final String id) {
        final MongoCollection<Document> c = this.db.getCollection("hosts");
        return c.find(new Document("id", id)).first();
    }

    public void removeHosts() {
        final MongoCollection<Document> c = this.db.getCollection("hosts");
        c.deleteMany(new Document());
    }

    public void upsertHost(final Document query, final Document update) {
        final MongoCollection<Document> c = this.db.getCollection("hosts");
        c.updateOne(query, new Document("$set", update), new UpdateOptions().upsert(true));
    }

    public boolean verifyUserHost(final String hostId) {
        Database.logger.info("Verify: " + hostId + "  -  " + this.user.toHexString());
        final MongoCollection<Document> c = this.db.getCollection("hosts");
        final Document query = new Document("id", hostId).append("$or",
                Arrays.asList(new Document("labels.efpi-type", "public"),
                        new Document("labels.efpi-type", "private").append("labels.efpi-user",
                                this.user.toHexString())));
        Database.logger.info(query.toJson());
        return c.find(query).first() != null;
    }

    public void deleteImage(final String image) {
        final MongoCollection<Document> c = this.db.getCollection("services");
        c.deleteOne(new Document("image", image));
    }

    public void updateImage(final String image, final List<Document> remaining) {
        final MongoCollection<Document> c = this.db.getCollection("services");
        c.updateOne(new Document("image", image), new Document("$set", new Document("tags", remaining)));
    }
}
