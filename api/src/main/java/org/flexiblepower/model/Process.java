/**
 * File Proces.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.model;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * Process
 *
 * @author coenvl
 * @version 0.1
 * @since Mar 30, 2017
 */
@Getter
@Setter
public class Process {

    private String id;

    private String userName;

    private String processService;

    private String runningNode;

    private List<Connection> connections;

}
