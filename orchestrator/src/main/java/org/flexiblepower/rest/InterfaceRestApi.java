package org.flexiblepower.rest;

import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.flexiblepower.api.InterfaceApi;
import org.flexiblepower.model.Interface;
import org.flexiblepower.orchestrator.RegistryConnector;

public class InterfaceRestApi extends BaseApi implements InterfaceApi {

    private final RegistryConnector registryConnector = new RegistryConnector();

    protected InterfaceRestApi(@Context final HttpHeaders httpHeaders, @Context final SecurityContext securityContext) {
        super(httpHeaders, securityContext);
    }

    @Override
    public List<Interface> listProtos() {
        return this.registryConnector.getInterfaces();
    }

    @Override
    public Response download(final String sha256) {
        final Interface itf = this.registryConnector.getInterface(sha256);
        if (itf == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        final ResponseBuilder builder = Response.ok(itf);
        builder.header("Content-Disposition", "attachment; filename=\"message.proto\"");
        return builder.build();
    }

    @Override
    public String newInterface(final Interface itf) {
        return this.registryConnector.addInterface(itf);
    }

    @Override
    public void deleteInterface(final String sha256) {
        this.registryConnector.deleteInterface(sha256);
    }
}
