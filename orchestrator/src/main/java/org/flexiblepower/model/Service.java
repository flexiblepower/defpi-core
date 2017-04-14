/**
 * File Service.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.model;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

/**
 * Service
 *
 * @author coenvl
 * @version 0.1
 * @since Mar 30, 2017
 */
@Getter
@Setter
public class Service {

    private String name;

    /**
     * Def-pi interfaces
     */
    private Set<Interface> interfaces;

    /**
     * Volume mappings
     */
    private Set<String> mappings;

    /**
     * Physical ports to open
     */
    private Set<String> ports;

    private String registry;

    private String image;

    private String tag;

    private String created;

    @JsonIgnore
    public String getFullImageName() {
        return this.registry + "/" + this.image + ":" + this.tag;
    }

    public String getPlatform() {
        return this.tag == null ? null : this.tag.endsWith("-arm") ? "arm" : "x86";
    }

    /**
     * @param image2
     */
    public void setFullImage(final String fullname) {
        final int pReg = fullname.indexOf('/');
        final int pTag = fullname.indexOf(':', pReg);
        final int pHash = fullname.indexOf('@', pTag);

        this.setRegistry(fullname.substring(0, pReg));
        this.setImage(fullname.substring(pReg + 1, pTag));
        this.setTag(fullname.substring(pTag + 1, pHash));
    }

}
