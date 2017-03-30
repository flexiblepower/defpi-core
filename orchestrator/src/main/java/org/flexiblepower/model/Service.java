/**
 * File Service.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.model;

import java.util.Set;

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

    private Set<Interface> interfaces;

    private Set<String> mappings;

    private Set<String> ports;

    private String image;

    private String tag;

    private String created;

    public String getPlatform() {
        return this.tag.endsWith("-arm") ? "arm" : "x86";
    }

}
