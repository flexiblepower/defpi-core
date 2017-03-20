package io.swagger.api.factories;

import io.swagger.api.NodeApiService;
import io.swagger.api.impl.NodeApiServiceImpl;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2017-03-20T14:11:17.160Z")
public class NodeApiServiceFactory {
    private final static NodeApiService service = new NodeApiServiceImpl();

    public static NodeApiService getNodeApi() {
        return service;
    }
}
