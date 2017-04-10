package org.flexiblepower.rest;

import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;

import org.flexiblepower.api.ServiceApi;
import org.flexiblepower.model.Service;
import org.flexiblepower.orchestrator.RegistryConnector;

public class ServiceRestApi extends BaseApi implements ServiceApi {

    protected final RegistryConnector registryConnector = new RegistryConnector();

    protected ServiceRestApi(@Context final HttpHeaders httpHeaders, @Context final SecurityContext securityContext) {
        super(httpHeaders, securityContext);
    }

    @Override
    public List<String> listRepositories() {
        return this.registryConnector.listRepositories();
    }

    @Override
    public List<String> listServices(final String repositoryName) {
        return this.registryConnector.listServices(repositoryName);
    }

    @Override
    public List<String> listTags(final String repositoryName, final String serviceName) {
        return this.registryConnector.listTags(repositoryName, serviceName);
    }

    @Override
    public Service getService(final String repositoryName, final String imageName, final String tag) {
        return this.registryConnector.getService(repositoryName, imageName, tag);
    }

    @Override
    public void deleteService(final String repositoryName, final String image, final String tag) {
        this.registryConnector.deleteService(repositoryName, image, tag);
    }

}
