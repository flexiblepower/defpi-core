package org.flexiblepower.plugin.servicegen.model;

import java.util.List;

import lombok.Data;

/**
 * Interface
 *
 * @author coenvl
 * @version 0.1
 * @since Apr 13, 2017
 */
@Data
public class Interface {

    private String name;
    private String classPrefix;
    private String descriptorClass;
    private int cardinality;
    private boolean autoConnect;
    private List<Type> subscribe;
    private List<Type> publish;

}
