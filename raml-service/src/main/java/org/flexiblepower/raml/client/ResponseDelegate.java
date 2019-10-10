package org.flexiblepower.raml.client;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

public class ResponseDelegate extends Response {
  private final Response delegate;

  private final Object entity;

  protected ResponseDelegate(Response delegate, Object entity) {
    this.delegate = delegate;
    this.entity = entity;
  }

  protected ResponseDelegate(Response delegate) {
    this(delegate, null);
  }

  @Override
  public int getLength() {
    return this.delegate.getLength();
  }

  @Override
  public URI getLocation() {
    return this.delegate.getLocation();
  }

  @Override
  public void close() {
    this.delegate.close();
  }

  @Override
  public Locale getLanguage() {
    return this.delegate.getLanguage();
  }

  @Override
  public Date getLastModified() {
    return this.delegate.getLastModified();
  }

  @Override
  public Date getDate() {
    return this.delegate.getDate();
  }

  @Override
  public MultivaluedMap<String, Object> getHeaders() {
    return this.delegate.getHeaders();
  }

  @Override
  public <T> T readEntity(Class<T> p0, Annotation[] p1) {
    return this.delegate.readEntity(p0,p1);
  }

  @Override
  public <T> T readEntity(GenericType<T> p0) {
    return this.delegate.readEntity(p0);
  }

  @Override
  public <T> T readEntity(GenericType<T> p0, Annotation[] p1) {
    return this.delegate.readEntity(p0,p1);
  }

  @Override
  public <T> T readEntity(Class<T> p0) {
    return this.delegate.readEntity(p0);
  }

  @Override
  public Response.StatusType getStatusInfo() {
    return this.delegate.getStatusInfo();
  }

  @Override
  public boolean hasEntity() {
    return this.delegate.hasEntity();
  }

  @Override
  public boolean bufferEntity() {
    return this.delegate.bufferEntity();
  }

  @Override
  public Set<String> getAllowedMethods() {
    return this.delegate.getAllowedMethods();
  }

  @Override
  public EntityTag getEntityTag() {
    return this.delegate.getEntityTag();
  }

  @Override
  public Link.Builder getLinkBuilder(String p0) {
    return this.delegate.getLinkBuilder(p0);
  }

  @Override
  public MultivaluedMap<String, String> getStringHeaders() {
    return this.delegate.getStringHeaders();
  }

  @Override
  public String getHeaderString(String p0) {
    return this.delegate.getHeaderString(p0);
  }

  @Override
  public MultivaluedMap<String, Object> getMetadata() {
    return this.delegate.getMetadata();
  }

  @Override
  public int getStatus() {
    return this.delegate.getStatus();
  }

  @Override
  public MediaType getMediaType() {
    return this.delegate.getMediaType();
  }

  @Override
  public Object getEntity() {
    return this.entity;}

  @Override
  public Set<Link> getLinks() {
    return this.delegate.getLinks();
  }

  @Override
  public Link getLink(String p0) {
    return this.delegate.getLink(p0);
  }

  @Override
  public boolean hasLink(String p0) {
    return this.delegate.hasLink(p0);
  }

  @Override
  public Map<String, NewCookie> getCookies() {
    return this.delegate.getCookies();
  }

  public static class HeaderBuilderBase {
    protected final Map<String, String> headerMap;

    protected HeaderBuilderBase() {
      this.headerMap = new HashMap<>();
    }

    public Response.ResponseBuilder toResponseBuilder(final Response.ResponseBuilder builder) {
      for (String s : headerMap.keySet())  {
        if (headerMap.get(s) != null )  {
          builder.header(s, headerMap.get(s));;
        }
      }
      return builder;
    }
  }
}
