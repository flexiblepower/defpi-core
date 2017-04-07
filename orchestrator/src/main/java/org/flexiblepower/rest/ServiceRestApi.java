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
    public List<String> listServices() {
        return this.registryConnector.getServices();
    }

    @Override
    public Service listService(final String imageName, final String tag) {
        return this.registryConnector.getService(imageName, tag);
    }

    @Override
    public void deleteService(final String image, final String tag) {
        this.registryConnector.deleteService(image, tag);
    }

}
