package org.flexiblepower.codegen.model;

import lombok.Data;

/**
 * Descriptor
 *
 * @author coenvl
 * @version 0.1
 * @since Apr 13, 2017
 */
@Data
public class Descriptor {

    private String url = null;
    private String file = "";
    private boolean upload = false;
    private String source = "";
}
