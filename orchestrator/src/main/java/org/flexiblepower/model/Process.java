/**
 * File Proces.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.model;

import java.util.List;

import lombok.Getter;

/**
 * Proces
 *
 * @author coenvl
 * @version 0.1
 * @since Mar 30, 2017
 */
@Getter
public class Process {

    private String userName;

    private Service processService;

    private List<Connection> connections;

}
