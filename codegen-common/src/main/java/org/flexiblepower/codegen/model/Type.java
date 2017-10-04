package org.flexiblepower.codegen.model;

import lombok.Data;

/**
 * Type
 *
 * @author coenvl
 * @version 0.1
 * @since Apr 13, 2017
 */
@Data
public class Type {

    private String descriptor;
    private String type;
    private Descriptor descriptorObject;

}
