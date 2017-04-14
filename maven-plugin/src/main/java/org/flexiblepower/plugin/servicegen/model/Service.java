package org.flexiblepower.plugin.servicegen.model;

import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * Service
 *
 * @author coenvl
 * @version 0.1
 * @since Apr 13, 2017
 */
@Data
public class Service {

    private String name;
    private Map<String, Descriptor> descriptors;
    private List<Interface> interfaces;
    private List<String> mappings;
    private List<String> ports;

}
