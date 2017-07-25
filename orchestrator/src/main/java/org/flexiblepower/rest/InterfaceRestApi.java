package org.flexiblepower.rest;

import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

import org.flexiblepower.api.InterfaceApi;
import org.flexiblepower.exceptions.NotFoundException;
import org.flexiblepower.model.Interface;
import org.flexiblepower.orchestrator.ServiceManager;

public class InterfaceRestApi extends BaseApi implements InterfaceApi {

    private final ServiceManager serviceManager;

    protected InterfaceRestApi(@Context final HttpHeaders httpHeaders) {
        super(httpHeaders);
        this.serviceManager = ServiceManager.getInstance();
    }

    @Override
    public List<Interface> listInterfaces() {
        return this.serviceManager.listInterfaces();
    }

    @Override
    public Interface getInterface(final String id) throws NotFoundException {
        return this.serviceManager.getInterfaceById(id);
    }

}
