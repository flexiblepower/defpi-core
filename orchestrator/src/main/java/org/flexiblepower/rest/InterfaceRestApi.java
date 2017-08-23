package org.flexiblepower.rest;

import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

import org.flexiblepower.api.InterfaceApi;
import org.flexiblepower.exceptions.AuthorizationException;
import org.flexiblepower.exceptions.NotFoundException;
import org.flexiblepower.model.Interface;
import org.flexiblepower.orchestrator.ServiceManager;

public class InterfaceRestApi extends BaseApi implements InterfaceApi {

    protected InterfaceRestApi(@Context final HttpHeaders httpHeaders) {
        super(httpHeaders);
    }

    @Override
    public List<Interface> listInterfaces() throws AuthorizationException {
        this.assertUserIsLoggedIn();
        return ServiceManager.getInstance().listInterfaces();
    }

    @Override
    public Interface getInterface(final String id) throws NotFoundException, AuthorizationException {
        this.assertUserIsLoggedIn();
        return ServiceManager.getInstance().getInterfaceById(id);
    }

}
