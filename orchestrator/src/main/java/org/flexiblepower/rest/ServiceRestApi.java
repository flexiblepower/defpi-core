package org.flexiblepower.rest;
// @Override
// public void deleteService(final String repositoryName, final String image, final String tag)
// throws ServiceNotFoundException,
// AuthorizationException {
// this.registryConnector.deleteService(repositoryName, image, tag);
// }

import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

import org.flexiblepower.api.ServiceApi;
import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.ServiceNotFoundException;
import org.flexiblepower.model.Service;
import org.flexiblepower.orchestrator.ServiceManager;

public class ServiceRestApi extends BaseApi implements ServiceApi {

    protected ServiceRestApi(@Context final HttpHeaders httpHeaders) {
        super(httpHeaders);
    }

    @Override
    public List<Service> listServices() throws AuthorizationException {
        this.assertUserIsLoggedIn();
        return ServiceManager.getInstance().listServices();
    }

    @Override
    public Service getService(final String id) throws ServiceNotFoundException, AuthorizationException {
        this.assertUserIsLoggedIn();
        return ServiceManager.getInstance().getService(id);
    }

}
