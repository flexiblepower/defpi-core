package org.flexiblepower.orchestrator;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.flexiblepower.api.ServiceApi;
import org.flexiblepower.model.Interface;
import org.flexiblepower.model.Service;
import org.json.JSONObject;

import com.google.gson.Gson;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RegistryConnector implements ServiceApi {

    public static final String REGISTRY_URL_KEY = "REGISTRY_URL";
    public static final String REGISTRY_URL_DFLT = "def-pi1.sensorlab.tno.nl:5000";
    public static final String REGISTRY_PREFIX = "dsefpi/";

    private final String registryApiLink;

    private final Gson gson = new Gson();

    public RegistryConnector() {
        String registryUrl = System.getenv(RegistryConnector.REGISTRY_URL_KEY);
        if (registryUrl == null) {
            registryUrl = RegistryConnector.REGISTRY_URL_DFLT;
        }
        this.registryApiLink = "https://" + registryUrl + "/v2/";
    }

    @Override
    public List<String> listRepositories() {
        final String textResponse = this.queryRegistry("_catalog");
        final List<String> allServices = this.gson.fromJson(textResponse, Catalog.class).getRepositories();

        final Set<String> ret = new HashSet<>();
        for (final String service : allServices) {
            final int p = service.indexOf("/");
            if (p > 0) {
                ret.add(service.substring(0, p));
            }
        }

        return new ArrayList<>(ret);
    }

    @Override
    public List<String> listServices(final String repository) {
        final String textResponse = this.queryRegistry("_catalog");
        final List<String> allServices = this.gson.fromJson(textResponse, Catalog.class).getRepositories();

        if ((repository == null) || repository.isEmpty() || repository.equals("all") || repository.equals("*")) {
            return allServices;
        } else {
            final Set<String> ret = new HashSet<>();
            for (final String service : allServices) {
                if (service.startsWith(repository)) {
                    ret.add(service.substring(service.indexOf("/") + 1));
                }
            }

            return new ArrayList<>(ret);
        }
    }

    @Override
    public List<String> listTags(final String repository, final String serviceName) {
        final String textResponse = this.queryRegistry(repository + "/" + serviceName + "/tags/list");
        return this.gson.fromJson(textResponse, TagList.class).getTags();
    }

    @Override
    public void deleteService(final String repository, final String serviceName, final String tag) {
        String queryPath = repository + "/" + serviceName + "/manifests/";

        try {
            queryPath = URLEncoder.encode(queryPath, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            RegistryConnector.log.warn("Unable to encode URL, assuming it is well-formed");
        }

        final String url = this.registryApiLink + queryPath + tag;

        RegistryConnector.log.debug("Requesting URL {}", url);
        final Client client = ClientBuilder.newClient();
        final WebTarget target = client.target(url);
        final Response response = target.request()
                .accept("application/vnd.docker.distribution.manifest.v2+json")
                .head();
        final String digest = response.getHeaderString("Docker-Content-Digest");
        RegistryConnector.log.info("Tag digest: " + digest);

        final String url2 = this.registryApiLink + queryPath + digest;
        RegistryConnector.log.debug("Deleting URL {}", url2);
        final WebTarget target2 = client.target(url2);
        final Response response2 = target2.request().delete();
        RegistryConnector.log.info(
                "Delete response: " + response2.getStatus() + "  -  " + response2.getStatusInfo().getReasonPhrase());

    }

    @Override
    @SuppressWarnings("unchecked")
    public Service getService(final String repository, final String serviceName, final String tag) {

        final String textResponse = this.queryRegistry(repository + "/" + serviceName + "/manifests/" + tag);

        final JSONObject v1Compatibility = new JSONObject(
                new JSONObject(textResponse).getJSONArray("history").getJSONObject(0).getString("v1Compatibility"));

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
    }

    /**
     * @param string
     * @return
     */
    private String queryRegistry(final String path) {
        String encoded_path = "/" + path;
        try {
            encoded_path = URLEncoder.encode(path, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            RegistryConnector.log.warn("Unable to encode URL, assuming it is well-formed");
        }

        final Client client = ClientBuilder.newClient();
        final String url = this.registryApiLink + encoded_path;
        RegistryConnector.log.debug("Requesting {}", url);

        final WebTarget target = client.target(url);
        final Response response = target.request().get();

        final String ret = response.readEntity(String.class);
        RegistryConnector.log.debug("Received response: {}", ret);
        return ret;
    }

    /**
     * @return
     */
    public List<Interface> getInterfaces() {
        return new ArrayList<>();
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

    @Getter
    private final class Catalog {

        private List<String> repositories;

    }

    @Getter
    private final class TagList {

        private String name;
        private List<String> tags;

    }

}
