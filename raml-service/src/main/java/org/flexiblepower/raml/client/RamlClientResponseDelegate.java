/**
 * File RamlClientResponseDelegate.java
 *
 * Copyright 2019 FAN
 */
package org.flexiblepower.raml.client;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Link.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.flexiblepower.proto.RamlProto.RamlResponse;

/**
 * RamlClientResponseDelegate
 *
 * @version 0.1
 * @param <T>
 * @since Oct 10, 2019
 */
public class RamlClientResponseDelegate extends Response {

    private final RamlResponse wrapped;
    private final Object entity;

    /**
     *
     */
    RamlClientResponseDelegate(final Object entity, final RamlResponse response) {
        this.wrapped = response;
        this.entity = entity;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.core.Response#getStatus()
     */
    @Override
    public int getStatus() {
        return this.wrapped.getStatus();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.core.Response#getStatusInfo()
     */
    @Override
    public StatusType getStatusInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.core.Response#getEntity()
     */
    @Override
    public Object getEntity() {
        return this.entity;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.core.Response#readEntity(java.lang.Class)
     */
    @Override
    public <T> T readEntity(final Class<T> entityType) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.core.Response#readEntity(javax.ws.rs.core.GenericType)
     */
    @Override
    public <T> T readEntity(final GenericType<T> entityType) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.core.Response#readEntity(java.lang.Class, java.lang.annotation.Annotation[])
     */
    @Override
    public <T> T readEntity(final Class<T> entityType, final Annotation[] annotations) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.core.Response#readEntity(javax.ws.rs.core.GenericType, java.lang.annotation.Annotation[])
     */
    @Override
    public <T> T readEntity(final GenericType<T> entityType, final Annotation[] annotations) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.core.Response#hasEntity()
     */
    @Override
    public boolean hasEntity() {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.core.Response#bufferEntity()
     */
    @Override
    public boolean bufferEntity() {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.core.Response#close()
     */
    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.core.Response#getMediaType()
     */
    @Override
    public MediaType getMediaType() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.core.Response#getLanguage()
     */
    @Override
    public Locale getLanguage() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.core.Response#getLength()
     */
    @Override
    public int getLength() {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.core.Response#getAllowedMethods()
     */
    @Override
    public Set<String> getAllowedMethods() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.core.Response#getCookies()
     */
    @Override
    public Map<String, NewCookie> getCookies() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.core.Response#getEntityTag()
     */
    @Override
    public EntityTag getEntityTag() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.core.Response#getDate()
     */
    @Override
    public Date getDate() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.core.Response#getLastModified()
     */
    @Override
    public Date getLastModified() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.core.Response#getLocation()
     */
    @Override
    public URI getLocation() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.core.Response#getLinks()
     */
    @Override
    public Set<Link> getLinks() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.core.Response#hasLink(java.lang.String)
     */
    @Override
    public boolean hasLink(final String relation) {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.core.Response#getLink(java.lang.String)
     */
    @Override
    public Link getLink(final String relation) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.core.Response#getLinkBuilder(java.lang.String)
     */
    @Override
    public Builder getLinkBuilder(final String relation) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.core.Response#getMetadata()
     */
    @Override
    public MultivaluedMap<String, Object> getMetadata() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.core.Response#getStringHeaders()
     */
    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.core.Response#getHeaderString(java.lang.String)
     */
    @Override
    public String getHeaderString(final String name) {
        // TODO Auto-generated method stub
        return null;
    }

}
