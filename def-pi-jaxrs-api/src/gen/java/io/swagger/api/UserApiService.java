package io.swagger.api;

import io.swagger.api.*;
import io.swagger.model.*;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

import io.swagger.model.Error;
import io.swagger.model.User;

import java.util.List;
import io.swagger.api.NotFoundException;

import java.io.InputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.validation.constraints.*;
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2017-03-20T14:11:17.160Z")
public abstract class UserApiService {
    public abstract Response createUser(SecurityContext securityContext) throws NotFoundException;
    public abstract Response deleteUser(String userId,SecurityContext securityContext) throws NotFoundException;
    public abstract Response getUserById(String userId,SecurityContext securityContext) throws NotFoundException;
    public abstract Response listUsers(SecurityContext securityContext) throws NotFoundException;
}
