/**
 * File RegistryConnector.java
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.text.ParseException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.flexiblepower.exceptions.ApiException;
import org.flexiblepower.exceptions.RepositoryNotFoundException;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.Architecture;
import org.flexiblepower.model.Interface;
import org.flexiblepower.model.Service;
import org.flexiblepower.model.Service.ServiceBuilder;
import org.flexiblepower.orchestrator.ServiceManager;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RegistryConnector
 *
 * @version 0.1
 * @since 20 mrt. 2017
 */
@Slf4j
public class RegistryConnector {

    /**
     * The key, or system variable name, which holds the registry URL to where the registry is from within the
     * orchestrator container
     */
    public static final String REGISTRY_URL_KEY = "REGISTRY_URL";
    private static final String REGISTRY_URL_DFLT = "registry:5000";

    /**
     * The key, or system variable name, which holds the registry URL to where the registry is from outside the
     * container. This is required when the registry is local, and hence the docker host needs to find it from
     * elsewhere.
     */
    public static final String REGISTRY_EXTERNAL_URL_KEY = "REGISTRY_EXTERNAL_URL";

    /**
     * The key, or system variable name, which holds a boolean that decides whether we need to use HTTPS to connect to
     * the registry.
     */
    public static final String SECURE_REGISTRY_KEY = "USE_SECURE_REGISTRY";
    private static final boolean SECURE_REGISTRY_DFLT = true;

    private static final long MAX_CACHE_AGE_MS = Duration.ofMinutes(2).toMillis();
    private static final long MAX_CACHE_REFRESH_TIME = Duration.ofSeconds(5).toMillis();

    private static RegistryConnector instance = null;
    private static int threadCount = 0;

    private final String registryName;
    private final String registryApiLink;
    private final ObjectMapper om = new ObjectMapper();

    private final ExecutorService cacheExecutor = Executors.newFixedThreadPool(10,
            r -> new Thread(r, "RegConThread" + RegistryConnector.threadCount++));
    private final Set<Interface> interfaceCache = new HashSet<>();

    private final Map<String, Service> serviceCache = new ConcurrentHashMap<>();
    private long serviceCacheLastUpdate = 0;
    private final Object serviceCacheLock = new Object();

    private final Map<String, Service> allServiceCache = new ConcurrentHashMap<>();
    private long allServiceCacheLastUpdate = 0;
    private final Object allServiceCacheLock = new Object();

    private RegistryConnector() {
        final String registryURLFromEnv = System.getenv(RegistryConnector.REGISTRY_URL_KEY);
        final String registryURL = (registryURLFromEnv != null ? registryURLFromEnv
                : RegistryConnector.REGISTRY_URL_DFLT);

        final String secureRegistryFromEnv = System.getenv(RegistryConnector.SECURE_REGISTRY_KEY);
        final boolean secureRegistry = (secureRegistryFromEnv != null ? Boolean.parseBoolean(secureRegistryFromEnv)
                : RegistryConnector.SECURE_REGISTRY_DFLT);
        this.registryApiLink = (secureRegistry ? "https://" : "http://") + registryURL + "/v2/";

        final String registryNameFromEnv = System.getenv(RegistryConnector.REGISTRY_EXTERNAL_URL_KEY);
        this.registryName = (registryNameFromEnv != null ? registryNameFromEnv : registryURL);
    }

    /**
     * @return The singleton instance of the ProcessConnector
     */
    public synchronized static RegistryConnector getInstance() {
        if (RegistryConnector.instance == null) {
            RegistryConnector.instance = new RegistryConnector();
        }
        return RegistryConnector.instance;
    }

    private List<String> listServiceNames(final String repository) throws RepositoryNotFoundException {
        try {
            RegistryConnector.log.info("Listing services in repository {}", repository);
            final String textResponse = RegistryConnector.queryRegistry(this.buildUrl("_catalog"));
            final List<String> allServices = this.om.readValue(textResponse, Catalog.class).getRepositories();

            if ((repository == null) || repository.isEmpty() || repository.equals("all") || repository.equals("*")) {
                // If the requested repository is null, empty, "all" or "*", we want to return any service we found
                return allServices;
            } else {
                final Set<String> ret = new HashSet<>();
                for (final String service : allServices) {
                    if (service.startsWith(repository)) {
                        ret.add(service.substring(repository.length() + 1));
                    }
                }

                return new ArrayList<>(ret);
            }
        } catch (final ServiceNotFoundException e) {
            throw new RepositoryNotFoundException(repository);
        } catch (final IOException e) {
            throw new RepositoryNotFoundException("Error parsing response", e);
        }
    }

    /**
     * List all service version that are available in the repository. When this takes longer than is allowed, the
     * remaining services are fetched and added to the cache in the background.
     * <p>
     * This implementation will cause bug where a REMOVED service will stay in the cache until the orchestrator is
     * restarted...
     *
     * @param repository The repository to look in
     * @return A collection of services that are in the repository
     * @throws RepositoryNotFoundException When the whole repository is not found
     */
    public Collection<Service> getAllServiceVersions(final String repository) throws RepositoryNotFoundException {
        synchronized (this.allServiceCacheLock) {
            final long cacheAge = System.currentTimeMillis() - this.allServiceCacheLastUpdate;
            if (cacheAge > RegistryConnector.MAX_CACHE_AGE_MS) {

                // Cache too old, refresh data
                Future<Future<?>> listTagsFuture = null;
                for (final String sn : this.listServiceNames(repository)) {
                    listTagsFuture = this.cacheExecutor.submit(() -> {
                        Future<?> listVersionFuture = null;
                        final List<String> tagsList = this.listTags(repository, sn);
                        final Map<String, List<String>> versions = RegistryConnector.groupTagVersions(tagsList);
                        for (final Entry<String, List<String>> e : versions.entrySet()) {
                            listVersionFuture = this.cacheExecutor.submit(() -> {
                                try {
                                    final String version = e.getKey();
                                    final Map<Architecture, String> tagsMap = RegistryConnector
                                            .tagsToArchitectureMap(e.getValue());
                                    final Service service = this
                                            .getServiceFromRegistry(repository, sn, version, tagsMap);
                                    final String key = service.getId() + ":" + service.getVersion();

                                    this.allServiceCache.put(key, service);
                                } catch (final ServiceNotFoundException ex) {
                                    RegistryConnector.log.error("Could not find service {}: {}", sn, ex.getMessage());
                                    RegistryConnector.log.trace(ex.getMessage(), ex);
                                }
                            });
                        }
                        return listVersionFuture;
                    });
                }

                try {
                    if (listTagsFuture != null) {
                        final Future<?> lastVersion = listTagsFuture.get(1, TimeUnit.SECONDS);
                        if (lastVersion != null) {
                            lastVersion.get(RegistryConnector.MAX_CACHE_REFRESH_TIME, TimeUnit.MILLISECONDS);
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    RegistryConnector.log.warn("Exception while fetching Services: {}", e.getClass());
                    RegistryConnector.log.trace(e.getMessage(), e);
                } catch (final TimeoutException e) {
                    RegistryConnector.log
                            .warn("Timeout while waiting for service versions, results may be incomplete!");
                }
                this.allServiceCacheLastUpdate = System.currentTimeMillis();
            }
        }
        return this.allServiceCache.values();
    }

    /**
     * Get the latest version of all services that are available in the repository. When this takes longer than is
     * allowed, the remaining services are fetched and added to the cache in the background.
     * <p>
     * This implementation will cause bug where a REMOVED service will stay in the cache until the orchestrator is
     * restarted...
     *
     * @param repository The repository to look in
     * @return A collection of services that are in the repository
     * @throws RepositoryNotFoundException When the whole repository is not found
     */
    public Collection<Service> getServices(final String repository) throws RepositoryNotFoundException {
        synchronized (this.serviceCacheLock) {
            final long cacheAge = System.currentTimeMillis() - this.serviceCacheLastUpdate;
            if (cacheAge > RegistryConnector.MAX_CACHE_AGE_MS) {
                // Cache too old, refresh data
                Future<Future<?>> listTagsFuture = null;
                for (final String serviceName : this.listServiceNames(repository)) {
                    listTagsFuture = this.cacheExecutor.submit(() -> {
                        final List<String> tagsList = this.listTags(repository, serviceName);
                        Collections.sort(tagsList);
                        final String version = (tagsList.contains("latest") ? "latest" : tagsList.get(0));
                        return this.cacheExecutor.submit(() -> {
                            try {
                                final Map<Architecture, String> tagsMap = RegistryConnector
                                        .tagsToArchitectureMap(tagsList);

                                final Service service = this
                                        .getServiceFromRegistry(repository, serviceName, version, tagsMap);
                                final String key = service.getId() + ":" + service.getVersion();

                                this.serviceCache.put(key,
                                        this.getServiceFromRegistry(repository, serviceName, version, tagsMap));
                            } catch (final ServiceNotFoundException ex) {
                                RegistryConnector.log
                                        .error("Could not find service {}: {}", serviceName, ex.getMessage());
                                RegistryConnector.log.trace(ex.getMessage(), ex);
                            }
                        });
                    });
                }

                try {
                    if (listTagsFuture != null) {
                        final Future<?> lastVersion = listTagsFuture.get(1, TimeUnit.SECONDS);
                        if (lastVersion != null) {
                            lastVersion.get(RegistryConnector.MAX_CACHE_REFRESH_TIME, TimeUnit.MILLISECONDS);
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    RegistryConnector.log.warn("Exception while fetching Services: {}", e.getClass());
                    RegistryConnector.log.trace(e.getMessage(), e);
                } catch (final TimeoutException e) {
                    RegistryConnector.log
                            .warn("Timeout while waiting for service versions, results may be incomplete!");
                }

                this.serviceCacheLastUpdate = System.currentTimeMillis();
            }
        }
        return this.serviceCache.values();
    }

    /**
     * @param value
     * @return
     */
    private static Map<Architecture, String> tagsToArchitectureMap(final List<String> tags) {
        final Map<Architecture, String> result = new HashMap<>();
        for (final String tag : tags) {
            result.put(Service.getArchitectureFromTag(tag), tag);
        }
        return result;
    }

    /**
     * @param tags
     * @return
     */
    private static Map<String, List<String>> groupTagVersions(final List<String> tags) {
        final Map<String, List<String>> result = new HashMap<>();
        for (final String tag : tags) {
            final String strippedTag;
            if (tag.contains("-")) {
                strippedTag = tag.substring(0, tag.indexOf("-"));
            } else {
                strippedTag = tag;
            }
            if (!result.containsKey(strippedTag)) {
                result.put(strippedTag, new ArrayList<>());
            }
            result.get(strippedTag).add(tag);
        }
        return result;
    }

    private List<String> listTags(final String repository, final String serviceName) {
        RegistryConnector.log.info("Listing tags from service {}/{}", repository, serviceName);
        try {
            final String textResponse = RegistryConnector
                    .queryRegistry(this.buildUrl(repository, serviceName, "tags", "list"));
            final List<String> ret = this.om.readValue(textResponse, TagList.class).getTags();
            return ret == null ? Collections.emptyList() : ret;
        } catch (final ServiceNotFoundException e) {
            RegistryConnector.log.error("Could not find service {}: {}", serviceName, e.getMessage());
            RegistryConnector.log.trace(e.getMessage(), e);
            return Collections.emptyList();
        } catch (final IOException e) {
            RegistryConnector.log.error("Error parsing tag list: {}", e.getMessage());
            RegistryConnector.log.trace(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get the service with the provided id from the repository.
     *
     * @param repository The repository to search in
     * @param id The ID of the service to obtain.
     * @return The service with the specified id
     * @throws ServiceNotFoundException When the service cannot be found
     */
    public Service getService(final String repository, final String id) throws ServiceNotFoundException {
        try {
            for (final Service service : this.getServices(repository)) {
                if (service.getId().equals(id)) {
                    return service;
                }
            }
        } catch (final RepositoryNotFoundException e) {
            throw new ServiceNotFoundException(e);
        }
        throw new ServiceNotFoundException(id);
    }

    private Service getServiceFromRegistry(final String repository,
            final String serviceName,
            final String version,
            final Map<Architecture, String> tags) throws ServiceNotFoundException {
        // final ISO8601DateFormat df = new ISO8601DateFormat();
        final StdDateFormat df = new StdDateFormat();
        final ServiceBuilder serviceBuilder = Service.builder();

        for (final Entry<Architecture, String> e : tags.entrySet()) {
            final String tag = e.getValue();
            final URI url = this.buildUrl(repository, serviceName, "manifests", tag);
            final String textResponse = RegistryConnector.queryRegistry(url);
            final JSONObject jsonResponse = new JSONObject(textResponse);

            final JSONObject v1Compatibility = new JSONObject(
                    jsonResponse.getJSONArray("history").getJSONObject(0).getString("v1Compatibility"));

            final JSONObject labels = v1Compatibility.getJSONObject("config").getJSONObject("Labels");

            try {
                serviceBuilder.created(df.parse(v1Compatibility.getString("created")));
            } catch (JSONException | ParseException e1) {
                serviceBuilder.created(new Date(0));
            }
            final String image = jsonResponse.getString("name");
            serviceBuilder.repository(ServiceManager.SERVICE_REPOSITORY);
            final String serviceId = image.substring(image.indexOf("/") + 1);
            serviceBuilder.id(serviceId);
            serviceBuilder.name(labels.getString("org.flexiblepower.serviceName"));

            try {
                final Set<Interface> interfaces = this.om.readValue(labels.getString("org.flexiblepower.interfaces"),
                        new TypeReference<Set<Interface>>() {
                            // A list of services
                        });
                serviceBuilder.interfaces(interfaces);

                // Add interfaces to the cache
                this.interfaceCache.addAll(interfaces);
            } catch (final IOException ex) {
                RegistryConnector.log.warn("Exception while parsing interface: {}", ex.getMessage());
                RegistryConnector.log.trace(ex.getMessage(), ex);
                continue;
            }
        }

        return serviceBuilder.tags(tags).registry(this.registryName).version(version).build();
    }

    private URI buildUrl(final String... pathParams) {
        final StringBuilder pathBuilder = new StringBuilder();
        String url = this.registryApiLink;
        try {
            for (final String p : pathParams) {
                pathBuilder.append(URLEncoder.encode(p, "UTF-8")).append('/');
            }
            url += pathBuilder.toString();
            url = url.substring(0, url.length() - 1);
        } catch (final UnsupportedEncodingException e) {
            RegistryConnector.log.warn("Unable to encode URL, assuming it is well-formed");
            url += String.join("/", pathParams);
        }
        return URI.create(url);
    }

    private static String queryRegistry(final URI uri) throws ServiceNotFoundException {
        RegistryConnector.log.debug("Requesting {}", uri);
        Response response = null;
        try {
            response = ClientBuilder.newClient().target(uri).request().get();
            RegistryConnector.validateResponse(response);
            return response.readEntity(String.class);
        } finally {
            if (response != null) {
                response.close();
            }
        }
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

    @Getter
    @NoArgsConstructor
    private static final class Catalog {

        private List<String> repositories;

    }

    @Getter
    @NoArgsConstructor
    private static final class TagList {

        private String name;
        private List<String> tags;

    }

}
