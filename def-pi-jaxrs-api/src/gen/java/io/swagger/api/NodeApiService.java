package io.swagger.api;

import io.swagger.api.*;
import io.swagger.model.*;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

import io.swagger.model.PrivateNode;
import io.swagger.model.PublicNode;
import io.swagger.model.UnidentifiedNode;

import java.util.List;
import io.swagger.api.NotFoundException;

import java.io.InputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.validation.constraints.*;
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2017-03-20T14:11:17.160Z")
public abstract class NodeApiService {
    public abstract Response createPrivateNode(SecurityContext securityContext) throws NotFoundException;
    public abstract Response createPublicNode(SecurityContext securityContext) throws NotFoundException;
    public abstract Response deletePriivateNode(String nodeId,SecurityContext securityContext) throws NotFoundException;
    public abstract Response deletePublicNode(String nodeId,SecurityContext securityContext) throws NotFoundException;
    public abstract Response getPrivateNode(String nodeId,SecurityContext securityContext) throws NotFoundException;
    public abstract Response getPublicNode(String nodeId,SecurityContext securityContext) throws NotFoundException;
    public abstract Response listPrivateNodes(SecurityContext securityContext) throws NotFoundException;
    public abstract Response listPublicNodes(SecurityContext securityContext) throws NotFoundException;
    public abstract Response listUnidentifiedNodes(SecurityContext securityContext) throws NotFoundException;
}
