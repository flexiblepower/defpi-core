package org.flexiblepower.orchestrator;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.flexiblepower.exceptions.ApiException;
import org.flexiblepower.exceptions.RepositoryNotFoundException;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.Interface;
import org.flexiblepower.model.Service;
import org.json.JSONObject;

import com.google.gson.Gson;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RegistryConnector {

    public static final String REGISTRY_URL_KEY = "REGISTRY_URL";
    private static final String REGISTRY_URL_DFLT = "def-pi1.sensorlab.tno.nl:5000";

    private final String registryApiLink;
    private final Gson gson = new Gson();

    private final Set<Interface> interfaceCache = new HashSet<>();

    public RegistryConnector() {
        String registryUrl = System.getenv(RegistryConnector.REGISTRY_URL_KEY);
        if (registryUrl == null) {
            registryUrl = RegistryConnector.REGISTRY_URL_DFLT;
        }
        this.registryApiLink = "https://" + registryUrl + "/v2/";
    }

    public List<String> listRepositories() {
        RegistryConnector.log.info("Listing all repositories");
        final Set<String> ret = new HashSet<>();
        try {
            final String textResponse = RegistryConnector.queryRegistry(this.buildUrl("_catalog"));
            final List<String> allServices = this.gson.fromJson(textResponse, Catalog.class).getRepositories();

            for (final String service : allServices) {
                final int p = service.indexOf("/");
                if (p > 0) {
                    ret.add(service.substring(0, p));
                }
            }
        } catch (final ServiceNotFoundException e) {
            // Error obtaining list from registry, no worries, return empty list
        }

        return new ArrayList<>(ret);
    }

    public List<String> listServices(final String repository) throws RepositoryNotFoundException {
        try {
            RegistryConnector.log.info("Listing services in repository {}", repository);
            final String textResponse = RegistryConnector.queryRegistry(this.buildUrl("_catalog"));
            final List<String> allServices = this.gson.fromJson(textResponse, Catalog.class).getRepositories();

            if ((repository == null) || repository.isEmpty() || repository.equals("all") || repository.equals("*")) {
                // If the requested repository is null, empty, "all" or "*", we want to return any service we found
                return allServices;
            } else {
                final Set<String> ret = new HashSet<>();
                for (final String service : allServices) {
                    if (service.startsWith(repository)) {
                        ret.add(service);
                    }
                }

                return new ArrayList<>(ret);
            }
        } catch (final ServiceNotFoundException e) {
            throw new RepositoryNotFoundException(repository);
        }
    }

    public List<String> listTags(final String repository, final String serviceName) throws ServiceNotFoundException {
        RegistryConnector.log.info("Listing tags from service {}/{}", repository, serviceName);
        final String textResponse = RegistryConnector
                .queryRegistry(this.buildUrl(repository, serviceName, "tags", "list"));
        final List<String> ret = this.gson.fromJson(textResponse, TagList.class).getTags();
        return ret == null ? Collections.emptyList() : ret;
    }

    public void deleteService(final String repository, final String serviceName, final String tag)
            throws ServiceNotFoundException {
        RegistryConnector.log.info("Deleting image from service {}/{}:{}", repository, serviceName, tag);

        // First get the digest, because we MUST send that to delete it
        final URI getUri = this.buildUrl(repository, serviceName, "manifests", tag);
        RegistryConnector.log.debug("Requesting URL {}", getUri);
        final Response response = ClientBuilder.newClient()
                .target(getUri)
                .request()
                .accept("application/vnd.docker.distribution.manifest.v2+json")
                .head();
        RegistryConnector.validateResponse(response);
        final String digest = response.getHeaderString("Docker-Content-Digest");

        // Now we have the digest, so we can delete it.
        final URI deleteUri = this.buildUrl(repository, serviceName, "manifests", digest);
        RegistryConnector.log.debug("Requesting URL {} (DELETE)", deleteUri);
        final Response response2 = ClientBuilder.newClient().target(deleteUri).request().delete();
        RegistryConnector.validateResponse(response2);
        RegistryConnector.log.debug("Delete response: {} ({})",
                response2.getStatusInfo().getReasonPhrase(),
                response2.getStatus());

    }

    public Service getService(final String fullImageName) throws ServiceNotFoundException {
        RegistryConnector.log.info("Obtaining service {}", fullImageName);
        final int p1 = fullImageName.lastIndexOf('/');
        final int p2 = fullImageName.lastIndexOf(':');

        return this.getService(fullImageName.substring(0, p1),
                fullImageName.substring(p1 + 1, p2),
                fullImageName.substring(p2 + 1));
    }

    public Service getService(final String repository, final String serviceName, final String tag)
            throws ServiceNotFoundException {
        RegistryConnector.log.info("Obtaining service {}/{}:{}", repository, serviceName, tag);
        final URI url = this.buildUrl(repository, serviceName, "manifests", tag);
        return this.getService(url);
    }

    @SuppressWarnings("unchecked")
    private Service getService(final URI url) throws ServiceNotFoundException {
        final String textResponse = RegistryConnector.queryRegistry(url);
        final JSONObject jsonResponse = new JSONObject(textResponse);

        final JSONObject v1Compatibility = new JSONObject(
                jsonResponse.getJSONArray("history").getJSONObject(0).getString("v1Compatibility"));

        final String created = v1Compatibility.getString("created");

        final JSONObject labels = v1Compatibility.getJSONObject("config").getJSONObject("Labels");

        final Set<Interface> interfaces = new LinkedHashSet<>();
        final Set<Object> set = this.gson.fromJson(labels.getString("org.flexiblepower.interfaces"), Set.class);
        for (final Object obj : set) {
            interfaces.add(this.gson.fromJson(this.gson.toJson(obj), Interface.class));
        }

        final Set<String> mappings = this.gson.fromJson(labels.getString("org.flexiblepower.mappings"), Set.class);
        final Set<String> ports = this.gson.fromJson(labels.getString("org.flexiblepower.ports"), Set.class);

        // Add interfaces to the cache
        this.interfaceCache.addAll(interfaces);

        final Service service = new Service();
        service.setName(labels.getString("org.flexiblepower.serviceName"));
        service.setRegistry(RegistryConnector.REGISTRY_URL_DFLT);
        service.setImage(jsonResponse.getString("name"));
        service.setTag(jsonResponse.getString("tag"));
        service.setInterfaces(interfaces);
        service.setCreated(created);
        service.setMappings(mappings);
        service.setPorts(ports);
        return service;
    }

    private URI buildUrl(final String... pathParams) {
        final StringBuilder pathBuilder = new StringBuilder();
        String url = this.registryApiLink;
        try {
            for (final String p : pathParams) {
                pathBuilder.append(URLEncoder.encode(p, "UTF-8")).append('/');
            }
            url += pathBuilder.toString();
        } catch (final UnsupportedEncodingException e) {
            RegistryConnector.log.warn("Unable to encode URL, assuming it is well-formed");
            url += String.join("/", pathParams);
        }
        return URI.create(url);
    }

    private static String queryRegistry(final URI uri) throws ServiceNotFoundException {
        RegistryConnector.log.debug("Requesting {}", uri);
        final Response response = ClientBuilder.newClient().target(uri).request().get();
        RegistryConnector.validateResponse(response);

        final String ret = response.readEntity(String.class);
        RegistryConnector.log.trace("Received response: {}", ret);
        return ret;
    }

    /**
     * @param response
     * @throws ServiceNotFoundException
     */
    private static void validateResponse(final Response response) throws ServiceNotFoundException {
        if (response.getStatusInfo().equals(Status.NOT_FOUND)) {
            throw new ServiceNotFoundException();
        } else if (response.getStatusInfo().getFamily() != Status.Family.SUCCESSFUL) {
            RegistryConnector.log.error("Unexpected response: {}", response);
            throw new ApiException(response.getStatus(), "Error obtaining service from registry");
        }
    }

    /**
     * @return
     */
    public List<Interface> getInterfaces() {
        // TODO: find ALL interfaces
        return new ArrayList<>(this.interfaceCache);
    }

    /**
     * @param sha256
     * @return
     */
    public Interface getInterface(final String sha256) {
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
