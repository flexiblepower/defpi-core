package org.flexiblepower.rest;

import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

import org.flexiblepower.api.ServiceApi;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.Service;
import org.flexiblepower.orchestrator.ServiceManager;

public class ServiceRestApi extends BaseApi implements ServiceApi {

    protected ServiceRestApi(@Context final HttpHeaders httpHeaders) {
        super(httpHeaders);
    }

    // @Override
    // public List<String> listRepositories() {
    // return this.registryConnector.listRepositories();
    // }

    @Override
    public List<Service> listServices() {
        return ServiceManager.getInstance().listServices();
    }

    // @Override
    // public List<String> listTags(final String repositoryName, final String serviceName)
    // throws ServiceNotFoundException {
    // return this.registryConnector.listTags(repositoryName, serviceName);
    // }

    @Override
    public Service getService(final String id) throws ServiceNotFoundException {
        return ServiceManager.getInstance().getService(id);
    }

    // @Override
    // public void deleteService(final String repositoryName, final String image, final String tag)
    // throws ServiceNotFoundException,
    // AuthorizationException {
    // this.registryConnector.deleteService(repositoryName, image, tag);
    // }

}
