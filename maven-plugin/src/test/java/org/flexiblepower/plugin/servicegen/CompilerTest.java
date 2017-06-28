/**
 * File CompilerTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.plugin.servicegen;

import org.junit.Test;

/**
 * CompilerTest
 *
 * @author coenvl
 * @version 0.1
 * @since Jun 27, 2017
 */
public class CompilerTest {

    @Test
    public void testCompiler() {
        System.out.println(ProtoCompiler.getOsName());
        System.out.println(ProtoCompiler.getArchitecture());
    }

}
