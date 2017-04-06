package org.flexiblepower.orchestrator;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Set;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.flexiblepower.model.Interface;
import org.flexiblepower.model.Service;
import org.json.JSONObject;

import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RegistryConnector {

    public static final String REGISTRY_URL_KEY = "REGISTRY_URL";
    public static final String REGISTRY_URL_DFLT = "hub.servicelab.org";
    public static final String REGISTRY_PREFIX = "dsefpi/";
    public static String registryUrl;
    public static String registryApiLink;

    private final Gson gson = new Gson();

    public RegistryConnector() {
        RegistryConnector.registryUrl = System.getenv(RegistryConnector.REGISTRY_URL_KEY);
        if (RegistryConnector.registryUrl == null) {
            RegistryConnector.registryUrl = RegistryConnector.REGISTRY_URL_DFLT;
        }
        RegistryConnector.registryApiLink = "https://" + RegistryConnector.registryUrl + "/v2/";
    }

    @SuppressWarnings("unchecked")
    public List<String> getServices() {
        final Client client = ClientBuilder.newClient();
        final WebTarget target = client.target(RegistryConnector.registryApiLink + "_catalog");
        final Response response = target.request().get();

        return this.gson.fromJson(response.readEntity(String.class), List.class);
    }

    public void deleteService(final String repository, final String tag) {
        try {
            final Client client = ClientBuilder.newClient();
            final WebTarget target = client.target(RegistryConnector.registryApiLink
                    + URLEncoder.encode(RegistryConnector.REGISTRY_PREFIX + repository, "UTF-8") + "/manifests/"
                    + URLEncoder.encode(tag, "UTF-8"));
            final Response response = target.request()
                    .accept("application/vnd.docker.distribution.manifest.v2+json")
                    .head();
            final String digest = response.getHeaderString("DockerConnector-Content-Digest");
            RegistryConnector.log.info("Tag digest: " + digest);
            final WebTarget target2 = client.target(RegistryConnector.registryApiLink
                    + URLEncoder.encode(RegistryConnector.REGISTRY_PREFIX + repository, "UTF-8") + "/manifests/"
                    + digest);
            final Response response2 = target2.request().delete();
            RegistryConnector.log.info("Delete response: " + response2.getStatus() + "  -  "
                    + response2.getStatusInfo().getReasonPhrase());

        } catch (final UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> getTags(final String repository) {
        try {
            final Client client = ClientBuilder.newClient();
            final WebTarget target = client.target(RegistryConnector.registryApiLink
                    + URLEncoder.encode(RegistryConnector.REGISTRY_PREFIX + repository, "UTF-8") + "/tags/list");

            final Response response = target.request().get();

            return this.gson.fromJson(response.readEntity(String.class), List.class);
        } catch (final UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public Service getService(final String repository, final String tag) {
        try {
            final Client client = ClientBuilder.newClient();
            final WebTarget target = client.target(RegistryConnector.registryApiLink
                    + URLEncoder.encode(RegistryConnector.REGISTRY_PREFIX + repository, "UTF-8") + "/manifests/"
                    + URLEncoder.encode(tag, "UTF-8"));
            final Response response = target.request().get();
            final String responseStr = response.readEntity(String.class);
            final JSONObject v1Compatibility = new JSONObject(
                    new JSONObject(responseStr).getJSONArray("history").getJSONObject(0).getString("v1Compatibility"));
            final String created = v1Compatibility.getString("created");
            final JSONObject labels = v1Compatibility.getJSONObject("config").getJSONObject("Labels");
            final Set<Interface> interfaces = this.gson.fromJson(labels.getString("org.flexiblepower.interfaces"),
                    Set.class);
            final Set<String> mappings = this.gson.fromJson(labels.getString("org.flexiblepower.mappings"), Set.class);
            final Set<String> ports = this.gson.fromJson(labels.getString("org.flexiblepower.ports"), Set.class);

            final Service service = new Service();
            service.setName(labels.getString("org.flexiblepower.serviceName"));
            service.setInterfaces(interfaces);
            service.setCreated(created);
            service.setMappings(mappings);
            service.setPorts(ports);
            return service;
        } catch (final UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @return
     */
    public List<Interface> getInterfaces() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @param sha256
     * @return
     */
    public Interface getInterface(final String sha256) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @param itf
     * @return
     */
    public String addInterface(final Interface itf) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @param sha256
     */
    public void deleteInterface(final String sha256) {
        // TODO Auto-generated method stub

    }

    // public static Document getInterfaceDocument(final String image, final String tag) {
    // final ServiceLabels serviceLabels = RegistryConnector.getLabels(
    // image.replace(RegistryConnector.registryLink, "").replace(RegistryConnector.registryPrefix, ""),
    // tag);
    //
    // return Document.parse("{\"interfaces\": " + InitGson.create().toJson(serviceLabels.getInterfaces()) + "}");
    // }
}
